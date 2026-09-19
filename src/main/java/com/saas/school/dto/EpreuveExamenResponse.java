package com.saas.school.dto;

import com.saas.school.entity.EpreuveExamen;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpreuveExamenResponse {

    private Long id;
    private Long examenId;

    private Long coefficientMatiereId;
    private String matiereNom;
    private Integer coefficient;
    private String niveauNom;
    private String serieNom; // null = toutes séries du niveau

    private Long creneauId;
    private LocalDate creneauDate;
    private LocalTime creneauHeureDebut;
    private LocalTime creneauHeureFin;

    private Integer dureeMinutes;

    public static EpreuveExamenResponse from(EpreuveExamen ep) {
        var cm = ep.getCoefficientMatiere();
        var creneau = ep.getCreneau();

        return EpreuveExamenResponse.builder()
                .id(ep.getId())
                .examenId(ep.getExamen() != null ? ep.getExamen().getId() : null)
                .coefficientMatiereId(cm != null ? cm.getId() : null)
                .matiereNom(cm != null && cm.getMatiere() != null ? cm.getMatiere().getNom() : null)
                .coefficient(cm != null ? cm.getCoefficient() : null)
                .niveauNom(cm != null && cm.getNiveau() != null ? cm.getNiveau().getNom() : null)
                .serieNom(cm != null && cm.getSerie() != null ? cm.getSerie().getNom() : null)
                .creneauId(creneau != null ? creneau.getId() : null)
                .creneauDate(creneau != null ? creneau.getDate() : null)
                .creneauHeureDebut(creneau != null ? creneau.getHeureDebut() : null)
                .creneauHeureFin(creneau != null ? creneau.getHeureFin() : null)
                .dureeMinutes(ep.getDureeMinutes())
                .build();
    }
}
