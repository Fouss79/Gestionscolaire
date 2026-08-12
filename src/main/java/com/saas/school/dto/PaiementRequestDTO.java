package com.saas.school.dto;

import lombok.Data;

@Data
public class PaiementRequestDTO {

    private Long inscriptionId;

    private String codeTypeFrais;

    private Integer mois; // null pour les frais annuels

    private Double montant;

    private String modePaiement;

    private String reference;
}