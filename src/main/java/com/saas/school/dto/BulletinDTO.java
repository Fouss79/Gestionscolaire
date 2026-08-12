package com.saas.school.dto;

import lombok.Data;

@Data
public class BulletinDTO {

    private Long eleveId;
    private String eleveNom;

    private String classeNom;
    private String annee;

    private Double moyenneTrimestre1;
    private Double moyenneTrimestre2;
    private Double moyenneTrimestre3;

    private Double moyenneAnnuelle;

    private Integer rang;
    private String mention;
}