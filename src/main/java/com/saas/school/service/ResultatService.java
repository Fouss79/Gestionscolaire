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
                .map(i -> mapToResultat(i, periode))
                .sorted(
                        Comparator.comparing(
                                ResultatEleveDTO::getMoyenneGenerale,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .toList();

        // Attribution du rang
        for (int idx = 0; idx < resultats.size(); idx++) {
            resultats.get(idx).setRang(idx + 1);
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
        // MATIÈRES DU BULLETIN
        // =========================================================
        //
        // IMPORTANT :
        // On utilise BulletinService.construireNotesPourBulletin()
        // au lieu de NoteService.getByInscriptionEtPeriode().
        //
        // Cette méthode part des matières programmées / affectées
        // à la classe et crée une Note vide si aucune note n'existe.
        //
        // Donc :
        // - matière notée       -> valeurs réelles
        // - matière non notée   -> valeurs null
        // - sous-groupe         -> respecté
        //
        // =========================================================

        Long classeId = inscription.getClasse() != null
                ? inscription.getClasse().getId()
                : null;

        Long anneeScolaireId = inscription.getAnneeScolaire() != null
                ? inscription.getAnneeScolaire().getId()
                : null;

        if (classeId == null || anneeScolaireId == null) {

            bulletin.setMatieres(List.of());
            bulletin.setTotalPoints(0.0);
            bulletin.setTotalCoefficients(0.0);
            bulletin.setMoyenneGenerale(null);
            bulletin.setAppreciation("-");

            return bulletin;
        }

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
        // TOTAUX
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
        // MOYENNE GÉNÉRALE
        // =========================================================

        Double moyenne =
                noteService.calculMoyennePeriode(
                        inscriptionId,
                        periode
                );

        bulletin.setMoyenneGenerale(moyenne);

        bulletin.setAppreciation(
                calculerAppreciation(moyenne)
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

        dto.setNoteClasse(
                note.getNClass()
        );

        dto.setNoteExamen(
                note.getNExem()
        );

        // =========================================================
        // COEFFICIENT
        // =========================================================

        dto.setCoefficient(
                note.getCoeff()
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

        // =========================================================
        // MOYENNE + POINTS
        // =========================================================
        //
        // Si aucune note n'existe :
        //
        // nClass = null
        // nExem  = null
        //
        // On ne doit PAS considérer cela comme une vraie note de 0.
        //
        // Le bulletin affichera donc "—".
        //
        // =========================================================

        if (note.getNClass() != null || note.getNExem() != null) {

            double nClass =
                    note.getNClass() != null
                            ? note.getNClass()
                            : 0.0;

            double nExem =
                    note.getNExem() != null
                            ? note.getNExem()
                            : 0.0;

            double moyenne =
                    (nClass + (nExem * 2)) / 3.0;

            dto.setMoyenne(moyenne);

            int coefficient =
                    note.getCoeff() != null
                            ? note.getCoeff()
                            : 1;

            dto.setPoints(
                    moyenne * coefficient
            );

        } else {

            dto.setMoyenne(null);
            dto.setPoints(null);
        }

        return dto;
    }


}