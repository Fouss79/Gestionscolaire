package com.saas.school.dto;

import com.saas.school.entity.Examen;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * Classes participant à l'examen.
     */
    private List<ExamenClasseResponse> classes;

    public static ExamenResponse from(Examen e) {

        List<ExamenClasseResponse> classes =
                e.getClasses() == null
                        ? List.of()
                        : e.getClasses()
                        .stream()
                        .map(ExamenClasseResponse::from)
                        .toList();

        return ExamenResponse.builder()
                .id(e.getId())
                .nom(e.getNom())

                .ecoleId(
                        e.getEcole() != null
                                ? e.getEcole().getId()
                                : null
                )

                .anneeScolaireId(
                        e.getAnneeScolaire() != null
                                ? e.getAnneeScolaire().getId()
                                : null
                )

                .anneeScolaireNom(
                        e.getAnneeScolaire() != null
                                ? e.getAnneeScolaire().getNom()
                                : null
                )

                .dateDebut(e.getDateDebut())
                .dateFin(e.getDateFin())
                .statut(e.getStatut())
                .createdAt(e.getCreatedAt())

                .classes(classes)

                .build();
    }
}