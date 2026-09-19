package com.saas.school.dto;

import com.saas.school.entity.Examen;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamenResponse {

    private Long id;
    private String nom;
    private Long ecoleId;
    private Long anneeScolaireId;
    private String anneeScolaireNom;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Examen.StatutExamen statut;
    private LocalDateTime createdAt;

    public static ExamenResponse from(Examen e) {
        return ExamenResponse.builder()
                .id(e.getId())
                .nom(e.getNom())
                .ecoleId(e.getEcole() != null ? e.getEcole().getId() : null)
                .anneeScolaireId(e.getAnneeScolaire() != null ? e.getAnneeScolaire().getId() : null)
                .anneeScolaireNom(e.getAnneeScolaire() != null ? e.getAnneeScolaire().getNom() : null)
                .dateDebut(e.getDateDebut())
                .dateFin(e.getDateFin())
                .statut(e.getStatut())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
