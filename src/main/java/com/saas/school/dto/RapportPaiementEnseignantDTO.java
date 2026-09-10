package com.saas.school.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class RapportPaiementEnseignantDTO {

    private Long enseignantId;
    private String enseignantNom;
    private String enseignantPrenom;
    private String matricule;
    private String typeContrat;

    private int totalHeures;
    private double totalMontantHeures;
    private double totalSalaireBase;
    private double totalMontant;

    private double totalPaye;
    private double totalEnAttente;

    private List<PaiementLigneDTO> paiements;

    @Data
    @Builder
    public static class PaiementLigneDTO {

        private Long id;

        private LocalDate periodeDebut;
        private LocalDate periodeFin;

        private int totalHeures;
        private double tauxHoraire;
        private double salaireBase;
        private double montantHeures;
        private double montant;

        private String statut;
        private LocalDate datePaiement;
    }
}