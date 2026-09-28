package com.saas.school.service;

import com.saas.school.dto.BulletinDtos;
import com.saas.school.dto.ResultatEleveDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import com.saas.school.util.Appreciation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Résultats / classement pour le cycle primaire, en miroir de
 * {@link ResultatService} (secondaire), avec les différences propres
 * au primaire :
 *
 * - notes sur 10 (noteMax = 10)
 * - une seule note par matière (nClass), pas de nClass/nExem
 * - moyenne pondérée par coefficient (comme BulletinPrimaireService),
 *   et non une simple moyenne arithmétique
 * - période = un MOIS (String), pas une "periode" de type "1ère Periode"
 *
 * Le calcul de la moyenne mensuelle réutilise exactement la même requête
 * (noteRepo.findNotesBulletinPrimaire) et le même programme
 * (coefRepo.findProgrammesPourClasse) que BulletinPrimaireService, pour
 * garantir que le classement correspond toujours à la moyenne affichée
 * sur le bulletin.
 */
@Service
@RequiredArgsConstructor
public class ResultatPrimaireService {

    private final InscriptionRepository inscriptionRepository;
    private final CoefficientMatiereRepository coefRepo;
    private final NoteRepository noteRepo;
    private final BulletinPrimaireService bulletinPrimaireService;

    private static final int NOTE_MAX = 10;

    /**
     * ============================================================
     * RÉSULTATS DE TOUTE UNE CLASSE (pour un mois donné)
     * ============================================================
     */
    @Transactional(readOnly = true)
    public List<ResultatEleveDTO> getResultatsClasse(
            Long classeId,
            Long anneeScolaireId,
            String mois
    ) {

        List<Inscription> inscriptions =
                inscriptionRepository.findActifsByClasseAndAnnee(
                        classeId,
                        anneeScolaireId,
                        StatutInscription.VALIDE
                );

        if (inscriptions.isEmpty()) {
            return List.of();
        }

        Inscription premiereInscription = inscriptions.get(0);
        Niveau niveau = premiereInscription.getClasse().getNiveau();

        List<CoefficientMatiere> programme =
                coefRepo.findProgrammesPourClasse(
                        premiereInscription.getEcole().getId(),
                        anneeScolaireId,
                        niveau.getId(),
                        classeId
                );

        List<ResultatEleveDTO> resultats =
                inscriptions.stream()
                        .map(inscription ->
                                calculerResultat(
                                        inscription,
                                        programme,
                                        anneeScolaireId,
                                        mois
                                )
                        )
                        .sorted(
                                Comparator.comparing(
                                        ResultatEleveDTO::getMoyenneGenerale,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                        )
                        .toList();

        attribuerRangs(resultats);

        return resultats;
    }

    /**
     * ============================================================
     * RÉSULTATS DE TOUTE UNE ÉCOLE (toutes classes primaires, un mois)
     * ============================================================
     *
     * NB : pas de rang inter-classes ici (comme pour le secondaire),
     * seulement un tri par moyenne décroissante.
     */
    @Transactional(readOnly = true)
    public List<ResultatEleveDTO> getResultatsEcole(
            Long ecoleId,
            Long anneeScolaireId,
            String mois
    ) {

        List<Inscription> inscriptions =
                inscriptionRepository.findActifsParEcoleEtAnnee(
                        ecoleId,
                        anneeScolaireId,
                        StatutInscription.VALIDE
                );

        // Regroupe par classe pour ne charger le programme
        // qu'une seule fois par classe (au lieu d'une fois par élève).
        Map<Long, List<Inscription>> parClasse =
                inscriptions.stream()
                        .filter(i -> i.getClasse() != null)
                        .collect(Collectors.groupingBy(
                                i -> i.getClasse().getId()
                        ));

        return parClasse.entrySet().stream()
                .flatMap(entry -> {

                    Long classeId = entry.getKey();
                    List<Inscription> insDeLaClasse = entry.getValue();
                    Inscription premiere = insDeLaClasse.get(0);
                    Niveau niveau = premiere.getClasse().getNiveau();

                    List<CoefficientMatiere> programme =
                            coefRepo.findProgrammesPourClasse(
                                    premiere.getEcole().getId(),
                                    anneeScolaireId,
                                    niveau.getId(),
                                    classeId
                            );

                    return insDeLaClasse.stream()
                            .map(inscription ->
                                    calculerResultat(
                                            inscription,
                                            programme,
                                            anneeScolaireId,
                                            mois
                                    )
                            );
                })
                .sorted(
                        Comparator.comparing(
                                ResultatEleveDTO::getMoyenneGenerale,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .toList();
    }

    /**
     * ============================================================
     * BULLETIN DÉTAILLÉ D'UN ÉLÈVE (primaire)
     * ============================================================
     *
     * Contrairement au secondaire (où ResultatService reconstruit le
     * détail matière par matière), BulletinPrimaireService.construire(...)
     * calcule déjà tout (lignes, moyenne, rang, effectif) pour toute la
     * classe : on l'appelle donc et on filtre l'élève demandé, plutôt
     * que de dupliquer ce calcul.
     */
    @Transactional(readOnly = true)
    public BulletinDtos getBulletinEleve(
            Long inscriptionId,
            Long classeId,
            Long anneeScolaireId,
            String mois
    ) {

        return bulletinPrimaireService
                .construire(classeId, anneeScolaireId, mois)
                .stream()
                .filter(b -> b.inscriptionId() != null
                        && b.inscriptionId().equals(inscriptionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Aucun bulletin trouvé pour l'inscription "
                                + inscriptionId
                                + " sur "
                                + mois
                ));
    }

    // ================================================================
    // CALCUL DU RÉSULTAT D'UN ÉLÈVE (moyenne pondérée du mois)
    // ================================================================

    private ResultatEleveDTO calculerResultat(
            Inscription inscription,
            List<CoefficientMatiere> programme,
            Long anneeScolaireId,
            String mois
    ) {

        ResultatEleveDTO dto = mapInformationsEleve(inscription);

        List<Note> notesTrouvees =
                noteRepo.findNotesBulletinPrimaire(
                        inscription.getId(),
                        anneeScolaireId,
                        mois
                );

        Map<Long, Note> notesParMatiere =
                notesTrouvees.stream()
                        .filter(n -> n.getCoefficientMatiere() != null)
                        .collect(Collectors.toMap(
                                n -> n.getCoefficientMatiere().getId(),
                                n -> n,
                                (a, b) -> a
                        ));

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal sommeCoefs = BigDecimal.ZERO;

        for (CoefficientMatiere cm : programme) {

            Note note = notesParMatiere.get(cm.getId());

            BigDecimal valeur =
                    note == null ? null : bd(note.getNClass());

            BigDecimal coef = bd(cm.getCoefficient());

            if (coef == null) {
                coef = BigDecimal.ONE;
            }

            if (valeur != null) {
                total = total.add(valeur.multiply(coef));
                sommeCoefs = sommeCoefs.add(coef);
            }
        }

        // Comme côté secondaire : pas de notes -> moyenne à 0.0
        // (jamais null), pour un tri/affichage cohérent.
        double moyenneGenerale =
                sommeCoefs.signum() > 0
                        ? total
                        .divide(sommeCoefs, 2, RoundingMode.HALF_UP)
                        .doubleValue()
                        : 0.0;

        dto.setMoyenneGenerale(moyenneGenerale);

        dto.setAppreciation(
                Appreciation.de(
                        BigDecimal.valueOf(moyenneGenerale),
                        NOTE_MAX
                )
        );

        return dto;
    }

    /**
     * ============================================================
     * ATTRIBUTION DU RANG — même moyenne = même rang (1, 2, 2, 4...)
     * ============================================================
     */
    private void attribuerRangs(List<ResultatEleveDTO> resultats) {

        int rang = 0;
        Double derniereMoyenne = null;

        for (int i = 0; i < resultats.size(); i++) {

            ResultatEleveDTO resultat = resultats.get(i);
            Double moyenne = resultat.getMoyenneGenerale();

            if (moyenne == null) {
                resultat.setRang(null);
                continue;
            }

            if (derniereMoyenne == null
                    || Double.compare(moyenne, derniereMoyenne) != 0) {

                rang = i + 1;
                derniereMoyenne = moyenne;
            }

            resultat.setRang(rang);
        }
    }

    /**
     * ============================================================
     * INFORMATIONS DE BASE D'UN ÉLÈVE
     * ============================================================
     */
    private ResultatEleveDTO mapInformationsEleve(Inscription inscription) {

        ResultatEleveDTO dto = new ResultatEleveDTO();

        dto.setInscriptionId(inscription.getId());

        if (inscription.getEleve() != null) {
            dto.setMatricule(inscription.getEleve().getMatricule());
            dto.setNom(inscription.getEleve().getNom());
            dto.setPrenom(inscription.getEleve().getPrenom());
        }

        if (inscription.getClasse() != null) {

            dto.setClasseNom(inscription.getClasse().getNomComplet());

            if (inscription.getClasse().getNiveau() != null) {

                dto.setNiveauNom(inscription.getClasse().getNiveau().getNom());

                if (inscription.getClasse().getNiveau().getCycle() != null) {
                    dto.setCycleNom(
                            inscription.getClasse().getNiveau().getCycle().getNom()
                    );
                }
            }
        }

        return dto;
    }

    /**
     * Conversion BigDecimal, Double, Integer...
     */
    private static BigDecimal bd(Number n) {
        return n == null ? null : new BigDecimal(n.toString());
    }
}