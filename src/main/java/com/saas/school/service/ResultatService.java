package com.saas.school.service;

import com.saas.school.dto.MatiereBulletinDTO;
import com.saas.school.dto.ResultatBulletinDTO;
import com.saas.school.dto.ResultatEleveDTO;
import com.saas.school.dto.NoteResponseDTO;
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
    private final NoteService noteService;
    private final BulletinService bulletinService;

    /**
     * Résultats de toute une classe pour une période donnée.
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

        List<ResultatEleveDTO> resultats = inscriptions.stream()
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
        // Élève A : 15.50 → 1er
        // Élève B : 14.00 → 2e
        // Élève C : 14.00 → 2e
        // Élève D : 12.50 → 4e
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
                    || Double.compare(moyenne, derniereMoyenne) != 0) {

                rang = i + 1;
                derniereMoyenne = moyenne;
            }

            resultat.setRang(rang);
        }

        return resultats;
    }
    /**
     * Résultats de toute une école.
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
                .map(i -> mapToResultat(i, periode))
                .toList();
    }
    private ResultatEleveDTO mapToResultatAvecBulletin(
            Inscription inscription,
            Long classeId,
            Long anneeScolaireId,
            String periode
    ) {

        ResultatEleveDTO dto =
                new ResultatEleveDTO();

        // =========================================================
        // INFORMATIONS ÉLÈVE
        // =========================================================

        dto.setInscriptionId(
                inscription.getId()
        );

        if (inscription.getEleve() != null) {

            dto.setMatricule(
                    inscription.getEleve().getMatricule()
            );

            dto.setNom(
                    inscription.getEleve().getNom()
            );

            dto.setPrenom(
                    inscription.getEleve().getPrenom()
            );
        }

        // =========================================================
        // CLASSE
        // =========================================================

        if (inscription.getClasse() != null) {

            dto.setClasseNom(
                    inscription.getClasse().getNomComplet()
            );

            if (inscription.getClasse().getNiveau() != null) {

                dto.setNiveauNom(
                        inscription.getClasse()
                                .getNiveau()
                                .getNom()
                );

                if (inscription.getClasse()
                        .getNiveau()
                        .getCycle() != null) {

                    dto.setCycleNom(
                            inscription.getClasse()
                                    .getNiveau()
                                    .getCycle()
                                    .getNom()
                    );
                }
            }
        }

        // =========================================================
        // RÉCUPÉRATION DES MATIÈRES PROGRAMMÉES
        // =========================================================

        List<Note> notesBulletin =
                bulletinService.construireNotesPourBulletin(
                        inscription,
                        classeId,
                        anneeScolaireId,
                        periode
                );

        // =========================================================
        // CALCUL DES POINTS ET COEFFICIENTS
        // =========================================================

        double totalPoints = 0.0;
        double totalCoefficients = 0.0;

        for (Note note : notesBulletin) {

            if (note == null) {
                continue;
            }

            // Note classe vide = 0
            double noteClasse =
                    note.getNClass() != null
                            ? note.getNClass()
                            : 0.0;

            // Note examen vide = 0
            double noteExamen =
                    note.getNExem() != null
                            ? note.getNExem()
                            : 0.0;

            // Coefficient vide = 0
            double coefficient =
                    note.getCoeff() != null
                            ? note.getCoeff()
                            : 0.0;

            // =====================================================
            // MOYENNE MATIÈRE
            // Classe = 1/3
            // Examen = 2/3
            // =====================================================

            double moyenne =
                    (noteClasse + (noteExamen * 2.0)) / 3.0;

            // =====================================================
            // POINTS
            // =====================================================

            double points =
                    moyenne * coefficient;

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

        // Arrondi à 2 décimales
        moyenneGenerale =
                Math.round(moyenneGenerale * 100.0) / 100.0;

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
        Inscription inscription = inscriptionRepository
                .findById(inscriptionId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Inscription introuvable : " + inscriptionId
                        )
                );

        ResultatBulletinDTO bulletin = new ResultatBulletinDTO();

        // =========================================================
        // INFORMATIONS ÉLÈVE
        // =========================================================

        bulletin.setInscriptionId(inscription.getId());

        if (inscription.getEleve() != null) {

            bulletin.setMatricule(
                    inscription.getEleve().getMatricule()
            );

            bulletin.setNom(
                    inscription.getEleve().getNom()
            );

            bulletin.setPrenom(
                    inscription.getEleve().getPrenom()
            );
        }

        // =========================================================
        // CLASSE / NIVEAU / CYCLE
        // =========================================================

        if (inscription.getClasse() != null) {

            bulletin.setClasseNom(
                    inscription.getClasse().getNomComplet()
            );

            if (inscription.getClasse().getNiveau() != null) {

                bulletin.setNiveauNom(
                        inscription.getClasse()
                                .getNiveau()
                                .getNom()
                );

                if (inscription.getClasse()
                        .getNiveau()
                        .getCycle() != null) {

                    bulletin.setCycleNom(
                            inscription.getClasse()
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
                    inscription.getAnneeScolaire().getNom()
            );
        }

        bulletin.setPeriode(periode);

        // =========================================================
        // IDENTIFIANTS
        // =========================================================

        Long classeId = inscription.getClasse() != null
                ? inscription.getClasse().getId()
                : null;

        Long anneeScolaireId = inscription.getAnneeScolaire() != null
                ? inscription.getAnneeScolaire().getId()
                : null;

        // =========================================================
        // VÉRIFICATION CLASSE / ANNÉE
        // =========================================================

        if (classeId == null || anneeScolaireId == null) {

            bulletin.setMatieres(List.of());
            bulletin.setTotalPoints(0.0);
            bulletin.setTotalCoefficients(0.0);
            bulletin.setMoyenneGenerale(0.0);
            bulletin.setAppreciation(
                    calculerAppreciation(0.0)
            );

            return bulletin;
        }

        // =========================================================
        // MATIÈRES DU BULLETIN
        // =========================================================
        //
        // On part des matières programmées dans la classe.
        //
        // Une matière sans note existe quand même dans le bulletin.
        //
        // Exemple :
        //
        // Mathématiques : 12 / 14 / coef 4
        // Français      : 10 / 12 / coef 3
        // Anglais       :  0 /  0 / coef 2
        // Histoire      :  0 /  0 / coef 0
        //
        // =========================================================

        List<Note> notesBulletin =
                bulletinService.construireNotesPourBulletin(
                        inscription,
                        classeId,
                        anneeScolaireId,
                        periode
                );

        // =========================================================
        // CONVERSION Note -> MatiereBulletinDTO
        // =========================================================

        List<MatiereBulletinDTO> matieres =
                notesBulletin.stream()
                        .filter(Objects::nonNull)
                        .map(this::mapNoteToMatiereBulletin)
                        .toList();

        bulletin.setMatieres(matieres);

        // =========================================================
        // CALCUL DES TOTAUX
        // =========================================================

        double totalPoints = matieres.stream()
                .mapToDouble(m ->
                        m.getPoints() != null
                                ? m.getPoints()
                                : 0.0
                )
                .sum();

        double totalCoefficients = matieres.stream()
                .mapToDouble(m ->
                        m.getCoefficient() != null
                                ? m.getCoefficient()
                                : 0.0
                )
                .sum();

        bulletin.setTotalPoints(totalPoints);
        bulletin.setTotalCoefficients(totalCoefficients);

        // =========================================================
        // CALCUL MOYENNE GÉNÉRALE
        // =========================================================
        //
        // IMPORTANT :
        //
        // On ne fait PLUS :
        //
        // noteService.calculMoyennePeriode(...)
        //
        // La moyenne est calculée à partir des matières du bulletin.
        //
        // Moyenne générale =
        //
        //       Total points
        // -----------------------
        //   Total coefficients
        //
        // =========================================================

        double moyenneGenerale;

        if (totalCoefficients > 0) {

            moyenneGenerale =
                    totalPoints / totalCoefficients;

        } else {

            moyenneGenerale = 0.0;
        }

        // Arrondi à 2 décimales
        moyenneGenerale =
                Math.round(moyenneGenerale * 100.0) / 100.0;

        bulletin.setMoyenneGenerale(moyenneGenerale);

        // =========================================================
        // APPRECIATION
        // =========================================================

        bulletin.setAppreciation(
                calculerAppreciation(moyenneGenerale)
        );

        // =========================================================
        // RANG
        // =========================================================
        //
        // Le rang continue d'être calculé avec les résultats
        // de la classe.
        //
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
                                r.getInscriptionId()
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
     * Conversion d'une note vers la ligne du bulletin.
     */
    private MatiereBulletinDTO mapToMatiereBulletin(
            NoteResponseDTO note
    ) {

        MatiereBulletinDTO dto =
                new MatiereBulletinDTO();

        dto.setMatiereId(
                note.getMatiereId()
        );

        dto.setMatiereNom(
                note.getMatiereNom()
        );

        dto.setCoefficient(
                note.getCoeff()
        );

        dto.setNoteClasse(
                note.getNClass()
        );

        dto.setNoteExamen(
                note.getNExem()
        );

        dto.setMoyenne(
                note.getMoyenne()
        );

        dto.setPoints(
                note.getPoints()
        );

        dto.setSousGroupeId(
                note.getSousGroupeId()
        );

        dto.setSousGroupeNom(
                note.getSousGroupeNom()
        );

        return dto;
    }

    private ResultatEleveDTO mapToResultat(
            Inscription inscription,
            String periode
    ) {

        ResultatEleveDTO dto =
                new ResultatEleveDTO();

        dto.setInscriptionId(
                inscription.getId()
        );

        dto.setMatricule(
                inscription.getEleve().getMatricule()
        );

        dto.setNom(
                inscription.getEleve().getNom()
        );

        dto.setPrenom(
                inscription.getEleve().getPrenom()
        );

        dto.setClasseNom(
                inscription.getClasse().getNomComplet()
        );

        if (inscription.getClasse().getNiveau() != null) {

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

        Double moyenne =
                noteService.calculMoyennePeriode(
                        inscription.getId(),
                        periode
                );

        dto.setMoyenneGenerale(moyenne);

        dto.setAppreciation(
                calculerAppreciation(moyenne)
        );

        return dto;
    }

    private String calculerAppreciation(Double moyenne) {

        if (moyenne == null) return "-";

        if (moyenne >= 16) return "Très Bien";

        if (moyenne >= 14) return "Bien";

        if (moyenne >= 12) return "Assez Bien";

        if (moyenne >= 10) return "Passable";

        return "Insuffisant";


    }
    private MatiereBulletinDTO mapNoteToMatiereBulletin(Note note) {

        MatiereBulletinDTO dto = new MatiereBulletinDTO();

        // ================= MATIÈRE =================
        if (note.getMatiere() != null) {
            dto.setMatiereId(note.getMatiere().getId());
            dto.setMatiereNom(note.getMatiere().getNom());
        }

        // ================= NOTES =================
        double noteClasse = note.getNClass() != null
                ? note.getNClass()
                : 0.0;

        double noteExamen = note.getNExem() != null
                ? note.getNExem()
                : 0.0;

        dto.setNoteClasse(noteClasse);
        dto.setNoteExamen(noteExamen);

        // ================= COEFFICIENT =================
        double coefficient = note.getCoeff() != null
                ? note.getCoeff()
                : 0.0;

        dto.setCoefficient(
                note.getCoeff() != null
                        ? note.getCoeff()
                        : 0
        );

        // ================= MOYENNE MATIÈRE =================
        double moyenne = (noteClasse + (noteExamen * 2)) / 3.0;

        dto.setMoyenne(moyenne);

        // ================= POINTS =================
        double points = moyenne * coefficient;

        dto.setPoints(points);

        // ================= SOUS-GROUPE =================
        if (note.getSousGroupe() != null) {
            dto.setSousGroupeId(note.getSousGroupe().getId());
            dto.setSousGroupeNom(note.getSousGroupe().getNom());
        }

        return dto;
    }
}