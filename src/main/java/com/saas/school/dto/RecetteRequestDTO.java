package com.saas.school.dto;

import lombok.Data;

@Data
public class RecetteRequestDTO {

    private Double montant;
    private String libelle;
    private String reference;
    private String modePaiement;
}