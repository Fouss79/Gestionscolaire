package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RemboursementEmpruntResponseDTO {

    private Long id;

    private Long empruntId;

    private String empruntLibelle;

    /**
     * Montant de ce remboursement
     */
    private Double montant;

    private String modePaiement;

    private String reference;

    private LocalDateTime dateRemboursement;

    /**
     * Montant total que l'école doit rembourser
     */
    private Double montantTotal;

    /**
     * Total déjà remboursé après ce remboursement
     */
    private Double montantRembourseTotal;

    /**
     * Reste à payer
     */
    private Double resteAPayer;

    /**
     * NON_PAYE, PARTIEL ou PAYE
     */
    private String statutPaiement;
}