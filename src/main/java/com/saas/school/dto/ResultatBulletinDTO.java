package com.saas.school.dto;

import lombok.Data;

import java.util.List;

@Data
public class ResultatBulletinDTO {

    private Long inscriptionId;

    private String matricule;
    private String nom;
    private String prenom;

    private String classeNom;
    private String niveauNom;
    private String cycleNom;

    private String anneeScolaire;
    private String periode;

    private Integer rang;

    private Double moyenneGenerale;
    private String appreciation;

    private Double totalPoints;
    private Double totalCoefficients;

    private List<MatiereBulletinDTO> matieres;
}