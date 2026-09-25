package com.saas.school.service;

import com.saas.school.dto.BulletinDtos;
import com.saas.school.dto.LigneBulletinDto;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import com.saas.school.util.Appreciation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Construit les bulletins mensuels du primaire.
 *
 * Notes : nClass, sur 10.
 * Moyenne : pondérée par les coefficients.
 * Classement : 1, 2, 2, 4.
 */
@Service
@RequiredArgsConstructor
public class BulletinPrimaireService {

    private final InscriptionRepository inscriptionRepo;
    private final CoefficientMatiereRepository coefRepo;
    private final NoteRepository noteRepo;
    private final BulletinMensuelInfoRepository infoRepo;

    @Transactional(readOnly = true)
    public List<BulletinDtos> construire(
            Long classeId,
            Long anneeId,
            String mois
    ) {

        List<Inscription> inscriptions =
                inscriptionRepo.findActifsByClasseAndAnnee(
                        classeId,
                        anneeId,
                        StatutInscription.VALIDE
                );

        if (inscriptions.isEmpty()) {
            return List.of();
        }

        Inscription premiereInscription = inscriptions.get(0);
        Niveau niveau = premiereInscription.getClasse().getNiveau();

        final int noteMax = 10;

        List<CoefficientMatiere> programme =
                coefRepo.findProgrammesPourClasse(
                        premiereInscription.getEcole().getId(),
                        anneeId,
                        niveau.getId(),
                        classeId
                );
        System.out.println(
                "PRIMAIRE | classeId=" + classeId
                        + " | anneeId=" + anneeId
                        + " | mois=" + mois
                        + " | nombre matières=" + programme.size()
        );

        List<BulletinDtos> bruts = new ArrayList<>();

        for (Inscription ins : inscriptions) {

            // 1. Récupérer les notes de l'élève pour ce mois
            List<Note> notesTrouvees =
                    noteRepo.findNotesBulletinPrimaire(
                            ins.getId(),
                            anneeId,
                            mois
                    );
            List<Note> toutesNotesInscription =
                    noteRepo.findByInscriptionIdAndInscription_AnneeScolaireId(
                            ins.getId(),
                            anneeId
                    );

            System.out.println(
                    "DIAGNOSTIC | inscriptionId=" + ins.getId()
                            + " | anneeId=" + anneeId
                            + " | TOUTES LES NOTES=" + toutesNotesInscription.size()
            );

            for (Note n : toutesNotesInscription) {
                System.out.println(
                        "DIAGNOSTIC NOTE"
                                + " | id=" + n.getId()
                                + " | periode=" + n.getPeriode()
                                + " | nClass=" + n.getNClass()
                                + " | nExem=" + n.getNExem()
                                + " | coefficientMatiereId="
                                + (n.getCoefficientMatiere() != null
                                ? n.getCoefficientMatiere().getId()
                                : null)
                                + " | anneeNote="
                                + (n.getAnneeScolaire() != null
                                ? n.getAnneeScolaire().getId()
                                : null)
                                + " | anneeInscription="
                                + (n.getInscription() != null
                                && n.getInscription().getAnneeScolaire() != null
                                ? n.getInscription().getAnneeScolaire().getId()
                                : null)
                );
            }

            // 2. Vérifier combien de notes ont été récupérées
            System.out.println(
                    "PRIMAIRE | inscriptionId=" + ins.getId()
                            + " | mois=" + mois
                            + " | nombre notes=" + notesTrouvees.size()
            );

            // 3. Afficher les valeurs enregistrées
            for (Note n : notesTrouvees) {
                System.out.println(
                        "NOTE | coefficientMatiereId="
                                + n.getCoefficientMatiere().getId()
                                + " | nClass=" + n.getNClass()
                );
            }

            // 4. Transformer les notes en Map comme auparavant
            Map<Long, Note> notes =
                    notesTrouvees.stream()
                            .collect(Collectors.toMap(
                                    n -> n.getCoefficientMatiere().getId(),
                                    n -> n,
                                    (a, b) -> a
                            ));
            List<LigneBulletinDto> lignes = new ArrayList<>();

            BigDecimal total = BigDecimal.ZERO;
            BigDecimal sommeCoefs = BigDecimal.ZERO;

            for (CoefficientMatiere cm : programme) {

                Note n = notes.get(cm.getId());
                System.out.println(
                        "MATIERE | coefficientMatiereId=" + cm.getId()
                                + " | note trouvée=" + (n != null)
                );

                BigDecimal valeur =
                        n == null ? null : bd(n.getNClass());

                BigDecimal coef = bd(cm.getCoefficient());

                if (coef == null) {
                    coef = BigDecimal.ONE;
                }

                if (valeur != null) {
                    total = total.add(
                            valeur.multiply(coef)
                    );

                    sommeCoefs = sommeCoefs.add(coef);
                }

                lignes.add(new LigneBulletinDto(
                        cm.getMatiere().getNom(),
                        valeur,
                        coef,
                        Appreciation.de(valeur, noteMax)
                ));
            }

            /*
             * Une absence de notes est différente
             * d'une véritable moyenne de 0/10.
             */
            BigDecimal moyenne =
                    sommeCoefs.signum() == 0
                            ? null
                            : total.divide(
                            sommeCoefs,
                            2,
                            RoundingMode.HALF_UP
                    );

            BulletinMensuelInfo info =
                    infoRepo.findByInscriptionIdAndMois(
                            ins.getId(),
                            mois
                    ).orElse(null);

            /*
             * CORRECTION 1 :
             * On conserve l'identifiant de l'inscription.
             */
            bruts.add(new BulletinDtos(
                    ins.getId(),
                    ins.getEleve().getNom()
                            + " "
                            + ins.getEleve().getPrenom(),
                    ins.getClasse().getNomComplet(),
                    mois,
                    info != null ? info.getAbsences() : 0,
                    lignes,
                    total.setScale(
                            2,
                            RoundingMode.HALF_UP
                    ),
                    moyenne,
                    info != null
                            && info.getObservationMaitre() != null
                            ? info.getObservationMaitre()
                            : "",
                    null,
                    null,
                    null,
                    noteMax
            ));
        }

        /*
         * Classement uniquement des élèves
         * ayant une moyenne mensuelle.
         *
         * Les élèves sans notes n'ont pas de rang.
         */
        List<BulletinDtos> tries = bruts.stream()
                .filter(b -> b.moyenne() != null)
                .sorted(
                        Comparator.comparing(
                                BulletinDtos::moyenne
                        ).reversed()
                )
                .toList();

        BigDecimal premier =
                tries.isEmpty()
                        ? null
                        : tries.get(0).moyenne();

        Map<BulletinDtos, Integer> rangs =
                new IdentityHashMap<>();

        int rang = 0;
        BigDecimal precedente = null;

        for (int i = 0; i < tries.size(); i++) {

            BulletinDtos b = tries.get(i);

            if (precedente == null
                    || b.moyenne().compareTo(precedente) != 0) {
                rang = i + 1;
            }

            precedente = b.moyenne();
            rangs.put(b, rang);
        }

        int effectif = bruts.size();

        /*
         * CORRECTION 2 :
         * On conserve inscriptionId après le classement.
         */
        return bruts.stream()
                .map(b -> new BulletinDtos(
                        b.inscriptionId(),
                        b.eleve(),
                        b.classe(),
                        b.mois(),
                        b.absences(),
                        b.lignes(),
                        b.total(),
                        b.moyenne(),
                        b.observationMaitre(),
                        premier,
                        rangs.get(b),
                        effectif,
                        b.noteMax()
                ))
                .toList();
    }

    /**
     * Conversion BigDecimal, Double, Integer...
     */
    private static BigDecimal bd(Number n) {
        return n == null
                ? null
                : new BigDecimal(n.toString());
    }
}