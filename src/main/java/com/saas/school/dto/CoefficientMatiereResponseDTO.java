package com.saas.school.dto;

import lombok.Data;

import java.util.List;

@Data
public class CoefficientMatiereResponseDTO {
    private Long id;
    private Long matiereId;
    private String matiereNom;
    private Long niveauId;
    private String niveauNom;
    private Long serieId;
    private String serieNom;
    private Integer coefficient;
    private Integer nombreHeuresParSemaine;
    private Long anneeScolaireId;
    private String anneeScolaireNom;
    private Long classeId;
    private Long sousGroupeId;
    private String classeNom;
     private String sousGroupeNom;
    private List<String> enseignantsAffectes; // noms des enseignants déjà affectés à cette ligne de programme



}