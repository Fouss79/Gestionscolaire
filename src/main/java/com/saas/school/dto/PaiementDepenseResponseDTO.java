package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaiementDepenseResponseDTO {

    private Long id;
    private Long depenseId;
    private String depenseLibelle;

    private Double montant;
    private String modePaiement;
    private String reference;
    private LocalDateTime datePaiement;

    // Situation de la dépense APRÈS ce paiement
    private Double montantTotal;
    private Double montantPayeTotal;
    private Double resteAPayer;
    private String statutPaiement;
}