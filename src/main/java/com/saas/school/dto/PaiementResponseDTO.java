package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaiementResponseDTO {
    private Long id;
    private Long inscriptionId;
    private String eleveNom;
    private String elevePrenom;
    private String classeNom;
    private String typeFraisCode;
    private String typeFraisLibelle;
    private Integer mois;
    private Integer annee;
    private String anneeScolaireNom; // ⚠️ nouveau — ex: "2025-2026", s'applique à TOUS les paiements
    private Double montant;
    private String modePaiement;
    private String reference;
    private LocalDateTime datePaiement;
}