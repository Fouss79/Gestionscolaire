package com.saas.school.dto;

import com.saas.school.entity.ExamenSalle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamenSalleResponse {

    private Long id;

    private Long examenId;

    private Long salleId;

    private String salleNom;

    private Integer capacite;

    private Boolean active;

    public static ExamenSalleResponse from(ExamenSalle entity) {

        var salle = entity.getSalle();

        return ExamenSalleResponse.builder()
                .id(entity.getId())
                .examenId(
                        entity.getExamen() != null
                                ? entity.getExamen().getId()
                                : null
                )
                .salleId(
                        salle != null
                                ? salle.getId()
                                : null
                )
                .salleNom(
                        salle != null
                                ? salle.getNom()
                                : null
                )
                .capacite(
                        salle != null
                                ? salle.getCapacite()
                                : null
                )
                .active(
                        salle != null
                                ? salle.isActive()
                                : null
                )
                .build();
    }
}