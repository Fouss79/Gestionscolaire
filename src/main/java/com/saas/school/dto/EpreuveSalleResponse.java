package com.saas.school.dto;

import com.saas.school.entity.EpreuveSalle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpreuveSalleResponse {

    private Long id;

    private Long epreuveId;

    private Long salleId;

    private String salleNom;

    private Integer capacite;

    public static EpreuveSalleResponse from(EpreuveSalle epreuveSalle) {

        var salle = epreuveSalle.getSalle();

        return EpreuveSalleResponse.builder()
                .id(epreuveSalle.getId())
                .epreuveId(
                        epreuveSalle.getEpreuve() != null
                                ? epreuveSalle.getEpreuve().getId()
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
                .build();
    }
}