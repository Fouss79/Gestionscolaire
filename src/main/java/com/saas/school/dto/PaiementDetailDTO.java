package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaiementDetailDTO {

    private Long id;
    private Double montant;
    private String modePaiement;
    private String reference;
    private LocalDateTime datePaiement;
    private Integer mois;
    private Integer annee;
}