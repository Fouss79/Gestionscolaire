package com.saas.school.service;

import com.saas.school.dto.MatiereBulletinDTO;
import com.saas.school.dto.ResultatBulletinDTO;
import com.saas.school.dto.ResultatEleveDTO;
import com.saas.school.entity.Inscription;
import com.saas.school.entity.Note;
import com.saas.school.repository.InscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ResultatService {

    private final InscriptionRepository inscriptionRepository;
    private final BulletinService bulletinService;

    /**
     * ============================================================
     * RÉSULTATS DE TOUTE UNE CLASSE
     * ============================================================
     *
     * Le calcul utilise exactement la même logique que le bulletin :
     *
     * - matière programmée = présente même sans note
     * - note classe vide = 0
     * - note examen vide = 0
     * - coefficient vide = 0
     * - moyenne matière = (classe + examen × 2) / 3
     * - points = moyenne × coefficient
     * - moyenne générale = total points / total coefficients
     *
     * Le rang est donc basé sur la même moyenne que celle
     * affichée dans le bulletin.
     */
    public List<ResultatEleveDTO> getResultatsClasse(
            Long classeId,
            Long anneeScolaireId,
            String periode
    ) {

        List<Inscription> inscriptions =
                inscriptionRepository
                        .findByClasseIdAndAnneeScolaire_Id(
                                classeId,
                                anneeScolaireId
                        );

        List<ResultatEleveDTO> resultats =
                inscriptions.stream()
                        .map(inscription ->
                                mapToResultatAvecBulletin(
                                        inscription,
                                        classeId,
                                        anneeScolaireId,
                                        periode
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

        // =========================================================
        // ATTRIBUTION DU RANG
        // =========================================================
        //
        // Même moyenne = même rang.
        //
        // Exemple :
        //
        // 15.50 → 1
        // 14.00 → 2
        // 14.00 → 2
        // 12.50 → 4
        //
        // =========================================================

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
                    || Double.compare(
                    moyenne,
                    derniereMoyenne
            ) != 0) {

                rang = i + 1;

                derniereMoyenne = moyenne;
            }

            resultat.setRang(rang);
        }

        return resultats;
    }

    /**
     * ============================================================
     * RÉSULTATS DE TOUTE UNE ÉCOLE
     * ============================================================
     */
    public List<ResultatEleveDTO> getResultatsEcole(
            Long ecoleId,
            Long anneeScolaireId,
            String periode
    ) {

        List<Inscription> inscriptions =
                inscriptionRepository
                        .findByEcoleIdAndAnneeScolaire_Id(
                                ecoleId,
                                anneeScolaireId
                        );

        return inscriptions.stream()
                .map(inscription -> {

                    Long classeId =
                            inscription.getClasse() != null
                                    ? inscription.getClasse().getId()
                                    : null;

                    Long anneeId =
                            inscription.getAnneeScolaire() != null
                                    ? inscription
                                    .getAnneeScolaire()
                                    .getId()
                                    : null;

                    if (classeId == null || anneeId == null) {
                        return mapInformationsEleve(
                                inscription,
                                0.0
                        );
                    }

                    return mapToResultatAvecBulletin(
                            inscription,
                            classeId,
                            anneeId,
                            periode
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
     * CALCUL DU RÉSULTAT D'UN ÉLÈVE
     * ============================================================
     */
    private ResultatEleveDTO mapToResultatAvecBulletin(
            Inscription inscription,
            Long classeId,
            Long anneeScolaireId,
            String periode
    ) {

        ResultatEleveDTO dto =
                mapInformationsEleve(
                        inscription,
                        0.0
                );

        // =========================================================
        // MATIÈRES PROGRAMMÉES
        // =========================================================

        List<Note> notesBulletin =
                bulletinService.construireNotesPourBulletin(
                        inscription,
                        classeId,
                        anneeScolaireId,
                        periode
                );

        // =========================================================
        // CALCUL
        // =========================================================

        double totalPoints = 0.0;
        double totalCoefficients = 0.0;

        for (Note note : notesBulletin) {

            if (note == null) {
                continue;
            }

            // -----------------------------------------------------
            // NOTE CLASSE
            // -----------------------------------------------------

            double noteClasse =
                    note.getNClass() != null
                            ? note.getNClass()
                            : 0.0;

            // -----------------------------------------------------
            // NOTE EXAMEN
            // -----------------------------------------------------

            double noteExamen =
                    note.getNExem() != null
                            ? note.getNExem()
                            : 0.0;

            // -----------------------------------------------------
            // COEFFICIENT
            // -----------------------------------------------------

            double coefficient =
                    note.getCoeff() != null
                            ? note.getCoeff()
                            : 0.0;

            // -----------------------------------------------------
            // MOYENNE MATIÈRE
            // -----------------------------------------------------
            //
            // Classe = 1/3
            // Examen = 2/3
            //
            // -----------------------------------------------------

            double moyenneMatiere =
                    (
                            noteClasse
                                    + (noteExamen * 2.0)
                    ) / 3.0;

            // -----------------------------------------------------
            // POINTS
            // -----------------------------------------------------

            double points =
                    moyenneMatiere * coefficient;

            totalPoints += points;
            totalCoefficients += coefficient;
        }

        // =========================================================
        // MOYENNE GÉNÉRALE
        // =========================================================

        double moyenneGenerale;

        if (totalCoefficients > 0) {

            moyenneGenerale =
                    totalPoints / totalCoefficients;

        } else {

            moyenneGenerale = 0.0;
        }

        // =========================================================
        // ARRONDI
        // =========================================================

        moyenneGenerale =
                Math.round(
                        moyenneGenerale * 100.0
                ) / 100.0;

        dto.setMoyenneGenerale(
                moyenneGenerale
        );

        dto.setAppreciation(
                calculerAppreciation(
                        moyenneGenerale
                )
        );

        return dto;
    }

    /**
     * ============================================================
     * BULLETIN DÉTAILLÉ D'UN ÉLÈVE
     * ============================================================
     */
    public ResultatBulletinDTO getBulletinEleve(
            Long inscriptionId,
            String periode
    ) {

        Inscription inscription =
                inscriptionRepository
                        .findById(inscriptionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Inscription introuvable : "
                                                + inscriptionId
                                )
                        );

        ResultatBulletinDTO bulletin =
                new ResultatBulletinDTO();

        // =========================================================
        // INFORMATIONS ÉLÈVE
        // =========================================================

        bulletin.setInscriptionId(
                inscription.getId()
        );

        if (inscription.getEleve() != null) {

            bulletin.setMatricule(
                    inscription
                            .getEleve()
                            .getMatricule()
            );

            bulletin.setNom(
                    inscription
                            .getEleve()
                            .getNom()
            );

            bulletin.setPrenom(
                    inscription
                            .getEleve()
                            .getPrenom()
            );
        }

        // =========================================================
        // CLASSE / NIVEAU / CYCLE
        // =========================================================

        if (inscription.getClasse() != null) {

            bulletin.setClasseNom(
                    inscription
                            .getClasse()
                            .getNomComplet()
            );

            if (inscription
                    .getClasse()
                    .getNiveau() != null) {

                bulletin.setNiveauNom(
                        inscription
                                .getClasse()
                                .getNiveau()
                                .getNom()
                );

                if (inscription
                        .getClasse()
                        .getNiveau()
                        .getCycle() != null) {

                    bulletin.setCycleNom(
                            inscription
                                    .getClasse()
                                    .getNiveau()
                                    .getCycle()
                                    .getNom()
                    );
                }
            }
        }

        // =========================================================
        // ANNÉE SCOLAIRE
        // =========================================================

        if (inscription.getAnneeScolaire() != null) {

            bulletin.setAnneeScolaire(
                    inscription
                            .getAnneeScolaire()
                            .getNom()
            );
        }

        bulletin.setPeriode(periode);

        // =========================================================
        // IDENTIFIANTS
        // =========================================================

        Long classeId =
                inscription.getClasse() != null
                        ? inscription
                        .getClasse()
                        .getId()
                        : null;

        Long anneeScolaireId =
                inscription.getAnneeScolaire() != null
                        ? inscription
                        .getAnneeScolaire()
                        .getId()
                        : null;

        // =========================================================
        // CLASSE / ANNÉE INVALIDE
        // =========================================================

        if (classeId == null
                || anneeScolaireId == null) {

            bulletin.setMatieres(
                    List.of()
            );

            bulletin.setTotalPoints(
                    0.0
            );

            bulletin.setTotalCoefficients(
                    0.0
            );

            bulletin.setMoyenneGenerale(
                    0.0
            );

            bulletin.setAppreciation(
                    calculerAppreciation(0.0)
            );

            return bulletin;
        }

        // =========================================================
        // MATIÈRES DU BULLETIN
        // =========================================================

        List<Note> notesBulletin =
                bulletinService.construireNotesPourBulletin(
                        inscription,
                        classeId,
                        anneeScolaireId,
                        periode
                );

        // =========================================================
        // CONVERSION
        // =========================================================

        List<MatiereBulletinDTO> matieres =
                notesBulletin.stream()
                        .filter(Objects::nonNull)
                        .map(this::mapNoteToMatiereBulletin)
                        .toList();

        bulletin.setMatieres(matieres);

        // =========================================================
        // TOTAUX
        // =========================================================

        double totalPoints =
                matieres.stream()
                        .mapToDouble(m ->
                                m.getPoints() != null
                                        ? m.getPoints()
                                        : 0.0
                        )
                        .sum();

        double totalCoefficients =
                matieres.stream()
                        .mapToDouble(m ->
                                m.getCoefficient() != null
                                        ? m.getCoefficient()
                                        : 0.0
                        )
                        .sum();

        bulletin.setTotalPoints(
                totalPoints
        );

        bulletin.setTotalCoefficients(
                totalCoefficients
        );

        // =========================================================
        // MOYENNE GÉNÉRALE
        // =========================================================

        double moyenneGenerale;

        if (totalCoefficients > 0) {

            moyenneGenerale =
                    totalPoints / totalCoefficients;

        } else {

            moyenneGenerale = 0.0;
        }

        moyenneGenerale =
                Math.round(
                        moyenneGenerale * 100.0
                ) / 100.0;

        bulletin.setMoyenneGenerale(
                moyenneGenerale
        );

        // =========================================================
        // APPRECIATION
        // =========================================================

        bulletin.setAppreciation(
                calculerAppreciation(
                        moyenneGenerale
                )
        );

        // =========================================================
        // RANG
        // =========================================================

        List<ResultatEleveDTO> resultats =
                getResultatsClasse(
                        classeId,
                        anneeScolaireId,
                        periode
                );

        ResultatEleveDTO resultatEleve =
                resultats.stream()
                        .filter(r ->
                                r.getInscriptionId() != null
                                        && r.getInscriptionId()
                                        .equals(inscriptionId)
                        )
                        .findFirst()
                        .orElse(null);

        if (resultatEleve != null) {

            bulletin.setRang(
                    resultatEleve.getRang()
            );
        }

        return bulletin;
    }

    /**
     * ============================================================
     * INFORMATIONS DE BASE D'UN ÉLÈVE
     * ============================================================
     */
    private ResultatEleveDTO mapInformationsEleve(
            Inscription inscription,
            Double moyenne
    ) {

        ResultatEleveDTO dto =
                new ResultatEleveDTO();

        dto.setInscriptionId(
                inscription.getId()
        );

        if (inscription.getEleve() != null) {

            dto.setMatricule(
                    inscription
                            .getEleve()
                            .getMatricule()
            );

            dto.setNom(
                    inscription
                            .getEleve()
                            .getNom()
            );

            dto.setPrenom(
                    inscription
                            .getEleve()
                            .getPrenom()
            );
        }

        if (inscription.getClasse() != null) {

            dto.setClasseNom(
                    inscription
                            .getClasse()
                            .getNomComplet()
            );

            if (inscription
                    .getClasse()
                    .getNiveau() != null) {

                dto.setNiveauNom(
                        inscription
                                .getClasse()
                                .getNiveau()
                                .getNom()
                );

                if (inscription
                        .getClasse()
                        .getNiveau()
                        .getCycle() != null) {

                    dto.setCycleNom(
                            inscription
                                    .getClasse()
                                    .getNiveau()
                                    .getCycle()
                                    .getNom()
                    );
                }
            }
        }

        dto.setMoyenneGenerale(
                moyenne
        );

        dto.setAppreciation(
                calculerAppreciation(
                        moyenne
                )
        );

        return dto;
    }

    /**
     * ============================================================
     * CONVERSION NOTE -> MATIÈRE BULLETIN
     * ============================================================
     */
    private MatiereBulletinDTO mapNoteToMatiereBulletin(
            Note note
    ) {

        MatiereBulletinDTO dto =
                new MatiereBulletinDTO();

        // =========================================================
        // MATIÈRE
        // =========================================================

        if (note.getMatiere() != null) {

            dto.setMatiereId(
                    note.getMatiere().getId()
            );

            dto.setMatiereNom(
                    note.getMatiere().getNom()
            );
        }

        // =========================================================
        // NOTES
        // =========================================================

        double noteClasse =
                note.getNClass() != null
                        ? note.getNClass()
                        : 0.0;

        double noteExamen =
                note.getNExem() != null
                        ? note.getNExem()
                        : 0.0;

        dto.setNoteClasse(
                noteClasse
        );

        dto.setNoteExamen(
                noteExamen
        );

        // =========================================================
        // COEFFICIENT
        // =========================================================

        Integer coefficient = note.getCoeff();

        /*
         * Si Note.coeff est null, on essaie de récupérer
         * le coefficient depuis le programme.
         */
        if (coefficient == null
                && note.getCoefficientMatiere() != null) {

            coefficient =
                    note.getCoefficientMatiere()
                            .getCoefficient();
        }

        if (coefficient == null) {
            coefficient = 0;
        }

        dto.setCoefficient(
                coefficient
        );

        // =========================================================
        // MOYENNE MATIÈRE
        // =========================================================

        double moyenne =
                (
                        noteClasse
                                + (noteExamen * 2.0)
                ) / 3.0;

        moyenne =
                Math.round(
                        moyenne * 100.0
                ) / 100.0;

        dto.setMoyenne(
                moyenne
        );

        // =========================================================
        // POINTS
        // =========================================================

        double points =
                moyenne * coefficient;

        points =
                Math.round(
                        points * 100.0
                ) / 100.0;

        dto.setPoints(
                points
        );

        // =========================================================
        // SOUS-GROUPE
        // =========================================================

        if (note.getSousGroupe() != null) {

            dto.setSousGroupeId(
                    note.getSousGroupe().getId()
            );

            dto.setSousGroupeNom(
                    note.getSousGroupe().getNom()
            );
        }

        return dto;
    }

    /**
     * ============================================================
     * APPRECIATION
     * ============================================================
     */
    private String calculerAppreciation(
            Double moyenne
    ) {

        if (moyenne == null) {
            return "-";
        }

        if (moyenne >= 16) {
            return "Très Bien";
        }

        if (moyenne >= 14) {
            return "Bien";
        }

        if (moyenne >= 12) {
            return "Assez Bien";
        }

        if (moyenne >= 10) {
            return "Passable";
        }

        return "Insuffisant";
    }
}