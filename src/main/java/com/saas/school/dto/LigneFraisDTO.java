package com.saas.school.dto;

import lombok.Data;

@Data
public class LigneFraisDTO {
    private Long id;
    private Long inscriptionId;
    private String eleveNom;
    private String elevePrenom;
    private String classeNom;
    private String typeFraisCode;
    private String typeFraisLibelle;
    private Integer mois;
    private Integer annee;
    private Double montantTotal;
    private Double montantPaye;
    private Double resteAPayer;
    private String statutPaiement;
    private boolean estimatif;
}