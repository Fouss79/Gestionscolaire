package com.saas.school.dto;

import java.util.List;

/**
 * =========================================================
 * DTOs dédiés à la saisie des notes du CYCLE PRIMAIRE.
 * =========================================================
 *
 * Fonctionnalité totalement isolée de la saisie de notes classique
 * (NoteRequest / NotesEnMasseRequest / NoteResponseDTO ne sont pas touchés).
 *
 * Pour le primaire :
 *  - la note est toujours sur 10 ;
 *  - seul Note.nClass est utilisé (Note.nExem reste null) ;
 *  - la "période" correspond à un mois (SEPTEMBRE, OCTOBRE, ...).
 */
public class NotesPrimaireDtos {

    public record NotePrimaireRequest(
            Long inscriptionId,
            Long coefficientMatiereId,
            Double note
    ) {}

    public record EnregistrementNotesPrimaireRequest(
            Long classeId,
            Long anneeScolaireId,
            String mois,
            List<NotePrimaireRequest> notes
    ) {}

    public record MatierePrimaireDto(
            Long coefficientMatiereId,
            Long matiereId,
            String matiereNom,
            Double coefficient,
            Integer ordreAffichage
    ) {}

    public record ElevePrimaireDto(
            Long inscriptionId,
            Long eleveId,
            String nom,
            String prenom,
            String matricule
    ) {}

    public record NotePrimaireDto(
            Long id,
            Long inscriptionId,
            Long coefficientMatiereId,
            Double note
    ) {}

    public record DonneesPrimaireDto(
            List<ElevePrimaireDto> eleves,
            List<MatierePrimaireDto> matieres,
            List<NotePrimaireDto> notes
    ) {}
}