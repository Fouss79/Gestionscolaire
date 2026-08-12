package com.saas.school.dto;

import lombok.Data;

@Data
public class ResultatEleveDTO {
    private Long inscriptionId;
    private String matricule;
    private String nom;
    private String prenom;
    private String classeNom;
    private String niveauNom;
    private String cycleNom;
    private Double moyenneGenerale;
    private String appreciation;
    private Integer rang;
}