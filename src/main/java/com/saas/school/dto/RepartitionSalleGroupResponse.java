package com.saas.school.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Vue groupée de la répartition : une salle et la liste des élèves
 * qui y sont affectés pour l'examen.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepartitionSalleGroupResponse {

    private Long salleId;
    private String salleNom;
    private Integer salleCapacite;

    private int nombreEleves;

    private List<RepartitionExamenResponse> eleves;
}