package com.saas.school.dto;

import lombok.Data;

@Data
public class RepartitionExamenDTO {

    private Long id;

    private Long examenId;

    private Long salleId;
    private String salleNom;

    private Integer numeroPlace;

    private Long inscriptionId;

    private Long eleveId;

    private String nom;
    private String prenom;

    private Long classeId;
    private String classeNom;
}