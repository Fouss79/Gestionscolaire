package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmpruntDTO {

    private Long id;

    /**
     * École concernée
     */
    private Long ecoleId;

    /**
     * Libellé de l'emprunt
     * Exemple : Prêt bancaire pour rénovation
     */
    private String libelle;

    /**
     * Personne ou organisme qui prête l'argent
     */
    private String preteur;

    /**
     * Description complémentaire
     */
    private String description;

    /**
     * Montant réellement reçu par l'école
     */
    private Double montantEmprunte;

    /**
     * Montant total que l'école devra rembourser.
     * Inclut le capital + intérêts/frais éventuels.
     */
    private Double montantARembourser;

    /**
     * Total déjà remboursé
     */
    private Double montantRembourse;

    /**
     * Montant restant à rembourser
     */
    private Double resteAPayer;

    /**
     * Date de l'emprunt
     */
    private LocalDateTime dateEmprunt;

    /**
     * Date prévue de fin de remboursement
     */
    private LocalDateTime dateEcheance;

    /**
     * NON_PAYE, PARTIEL ou PAYE
     */
    private String statutPaiement;
}