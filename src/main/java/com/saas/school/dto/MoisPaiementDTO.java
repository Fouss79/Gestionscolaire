package com.saas.school.dto;


import lombok.Data;

@Data
public class MoisPaiementDTO {
    private Integer mois;
    private Integer annee;
    private Double montantDu;
    private Double montantPaye;
    private Double resteAPayer;
    private String statut; // PAYE / PARTIEL / NON_PAYE
}