package com.saas.school.dto;

import lombok.Data;

import java.util.List;

@Data
public class ClasseResponseDTO {
    private Long id;
    private String niveauNom;
    private String serieNom;
    private String groupeNom;
    private String nomComplet;
    private Long ecoleId;
    private int nbElevesInscrits;
    private int nbElevesValides;
    private List<SousGroupeResumeDTO> sousGroupes; // ⚠️ nouveau
}