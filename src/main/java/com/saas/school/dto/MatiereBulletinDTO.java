package com.saas.school.dto;

import lombok.Data;

@Data
public class MatiereBulletinDTO {

    private Long matiereId;
    private String matiereNom;

    private Integer coefficient;

    private Double noteClasse;
    private Double noteExamen;

    private Double moyenne;
    private Double points;

    private Long sousGroupeId;
    private String sousGroupeNom;
}