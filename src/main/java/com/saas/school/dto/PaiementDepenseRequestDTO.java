package com.saas.school.dto;

import lombok.Data;

@Data
public class PaiementDepenseRequestDTO {

    private Long depenseId;
    private Double montant;
    private String modePaiement;
    private String reference;
}