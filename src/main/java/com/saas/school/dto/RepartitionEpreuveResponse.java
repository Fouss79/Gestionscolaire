package com.saas.school.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepartitionEpreuveResponse {

    private Long id;

    private Long epreuveId;

    private Long inscriptionId;

    private Long eleveId;

    private String eleveNom;

    private String elevePrenom;

    private Long classeId;

    private String classeNom;

    private Long salleId;

    private String salleNom;

    private Integer salleCapacite;
}