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

        System.out.println("==============================================");
        System.out.println("PRIMAIRE | CONSTRUCTION BULLETIN");
        System.out.println("classeId = " + classeId);
        System.out.println("anneeId = " + anneeId);
        System.out.println("mois = " + mois);
        System.out.println("nombre matières = " + programme.size());
        System.out.println("==============================================");

        List<BulletinDtos> bruts = new ArrayList<>();

        for (Inscription ins : inscriptions) {

            // =========================================================
            // 🔎 DIAGNOSTIC : TOUTES LES NOTES DE L'INSCRIPTION
            // =========================================================

            List<Note> toutesNotesInscription =
                    noteRepo.findByInscriptionIdAndInscription_AnneeScolaireId(
                            ins.getId(),
                            anneeId
                    );

            System.out.println("----------------------------------------------");
            System.out.println("🔎 DIAGNOSTIC NOTES PRIMAIRE");
            System.out.println("inscriptionId = " + ins.getId());
            System.out.println("élève = "
                    + ins.getEleve().getNom()
                    + " "
                    + ins.getEleve().getPrenom());
            System.out.println("anneeId = " + anneeId);
            System.out.println("mois recherché = " + mois);
            System.out.println(
                    "NOMBRE TOTAL DE NOTES = "
                            + toutesNotesInscription.size()
            );

            // Afficher toutes les périodes réellement enregistrées
            System.out.println(
                    "PERIODES ENREGISTREES = "
                            + toutesNotesInscription.stream()
                            .map(Note::getPeriode)
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList()
            );

            // Afficher le détail de chaque note
            for (Note n : toutesNotesInscription) {

                System.out.println(
                        "DIAGNOSTIC NOTE"
                                + " | id=" + n.getId()
                                + " | periode=" + n.getPeriode()
                                + " | nClass=" + n.getNClass()
                                + " | nExem=" + n.getNExem()
                                + " | coeff=" + n.getCoeff()
                                + " | coefficientMatiereId="
                                + (
                                n.getCoefficientMatiere() != null
                                        ? n.getCoefficientMatiere().getId()
                                        : null
                        )
                                + " | matiere="
                                + (
                                n.getMatiere() != null
                                        ? n.getMatiere().getNom()
                                        : null
                        )
                                + " | anneeNote="
                                + (
                                n.getAnneeScolaire() != null
                                        ? n.getAnneeScolaire().getId()
                                        : null
                        )
                                + " | anneeInscription="
                                + (
                                n.getInscription() != null
                                        && n.getInscription().getAnneeScolaire() != null
                                        ? n.getInscription()
                                        .getAnneeScolaire()
                                        .getId()
                                        : null
                        )
                );
            }

            System.out.println("----------------------------------------------");

            // =========================================================
            // 📚 RÉCUPÉRATION DES NOTES POUR LE MOIS
            // =========================================================

            List<Note> notesTrouvees =
                    noteRepo.findNotesBulletinPrimaire(
                            ins.getId(),
                            anneeId,
                            mois
                    );

            System.out.println(
                    "PRIMAIRE | inscriptionId=" + ins.getId()
                            + " | mois=" + mois
                            + " | nombre notes="
                            + notesTrouvees.size()
            );

            // =========================================================
            // 📝 AFFICHER LES NOTES TROUVÉES POUR CE MOIS
            // =========================================================

            for (Note n : notesTrouvees) {

                System.out.println(
                        "NOTE TROUVEE"
                                + " | id=" + n.getId()
                                + " | periode=" + n.getPeriode()
                                + " | coefficientMatiereId="
                                + (
                                n.getCoefficientMatiere() != null
                                        ? n.getCoefficientMatiere().getId()
                                        : null
                        )
                                + " | matiere="
                                + (
                                n.getMatiere() != null
                                        ? n.getMatiere().getNom()
                                        : null
                        )
                                + " | nClass=" + n.getNClass()
                                + " | nExem=" + n.getNExem()
                );
            }

            // =========================================================
            // 📊 TRANSFORMATION EN MAP
            // =========================================================

            Map<Long, Note> notes =
                    notesTrouvees.stream()
                            .filter(n -> n.getCoefficientMatiere() != null)
                            .collect(Collectors.toMap(
                                    n -> n.getCoefficientMatiere().getId(),
                                    n -> n,
                                    (a, b) -> a
                            ));

            List<LigneBulletinDto> lignes = new ArrayList<>();

            BigDecimal total = BigDecimal.ZERO;
            BigDecimal sommeCoefs = BigDecimal.ZERO;

            // =========================================================
            // 📚 PARCOURIR LE PROGRAMME
            // =========================================================

            for (CoefficientMatiere cm : programme) {

                Note n = notes.get(cm.getId());

                System.out.println(
                        "MATIERE"
                                + " | coefficientMatiereId=" + cm.getId()
                                + " | matiere="
                                + (
                                cm.getMatiere() != null
                                        ? cm.getMatiere().getNom()
                                        : null
                        )
                                + " | note trouvée=" + (n != null)
                );

                BigDecimal valeur =
                        n == null
                                ? null
                                : bd(n.getNClass());

                BigDecimal coef =
                        bd(cm.getCoefficient());

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
                        Appreciation.de(
                                valeur,
                                noteMax
                        )
                ));
            }

            // =========================================================
            // 📊 MOYENNE MENSUELLE
            // =========================================================

            BigDecimal moyenne =
                    sommeCoefs.signum() == 0
                            ? null
                            : total.divide(
                            sommeCoefs,
                            2,
                            RoundingMode.HALF_UP
                    );

            System.out.println(
                    "MOYENNE PRIMAIRE"
                            + " | inscriptionId=" + ins.getId()
                            + " | mois=" + mois
                            + " | total=" + total
                            + " | sommeCoefs=" + sommeCoefs
                            + " | moyenne=" + moyenne
            );

            // =========================================================
            // 📝 INFORMATIONS BULLETIN
            // =========================================================

            BulletinMensuelInfo info =
                    infoRepo.findByInscriptionIdAndMois(
                            ins.getId(),
                            mois
                    ).orElse(null);

            // =========================================================
            // 📄 CONSTRUCTION DU BULLETIN
            // =========================================================

            bruts.add(new BulletinDtos(
                    ins.getId(),
                    ins.getEleve().getNom()
                            + " "
                            + ins.getEleve().getPrenom(),
                    ins.getClasse().getNomComplet(),
                    mois,
                    info != null
                            ? info.getAbsences()
                            : 0,
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

        // =========================================================
        // 🏆 CLASSEMENT
        // =========================================================

        List<BulletinDtos> tries =
                bruts.stream()
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

        // =========================================================
        // 🔄 RETOUR FINAL
        // =========================================================

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
