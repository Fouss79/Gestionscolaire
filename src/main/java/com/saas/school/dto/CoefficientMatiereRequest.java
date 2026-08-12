package com.saas.school.dto;

import lombok.Data;

@Data
public class CoefficientMatiereRequest {
    private Long matiereId;
    private Long niveauId;
    private Long serieId; // optionnel
    private Integer coefficient;
    private Long ecoleId;
    private Integer nombreHeuresParSemaine;
    private Long anneeScolaireId;
    private  String anneeScolaireNom;
    private Long classeId;
    private Long sousGroupeId;

}