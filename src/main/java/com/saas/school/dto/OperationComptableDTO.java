package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO à double usage :
 *  - une OPÉRATION individuelle (recette ou dépense), via mapToDTO()
 *  - le RAPPORT global agrégé d'une école, via genererRapport()
 *
 * Les champs "rapport" (totalRecettes, totalDepenses, solde,
 * nombreOperations, operations) ne sont renseignés que sur l'objet
 * racine retourné par genererRapport() ; ils restent null/vides sur
 * chaque opération individuelle de la liste.
 */
@Data
public class OperationComptableDTO {

    // ===== Identité =====
    private Long id;
    private Long ecoleId;

    // ===== Informations générales =====
    private String libelle;
    private Double montant;
    private LocalDateTime dateOperation;
    private String reference;
    private String modePaiement;
    private String nature; // RECETTE / DEPENSE

    // ===== Typage métier de l'opération =====
    private String typeOperation; // "PAIEMENT_SCOLARITE" ou "DEPENSE"
    private Long referenceId;     // id de l'entité source (ex: paiementScolariteId)

    // ===== Dépense =====
    private Long categorieDepenseId;
    private String categorieDepenseNom;

    // ===== Recette issue d'un paiement de scolarité =====
    private Long paiementScolariteId;
    private Long inscriptionId;
    private String eleveNom;
    private String elevePrenom;
    private String typeFraisNom;

    // ===== Rapport comptable global (uniquement sur l'objet racine) =====
    private Double totalRecettes;
    private Double totalDepenses;
    private Double solde;
    private Integer nombreOperations;
    private List<OperationComptableDTO> operations;
}