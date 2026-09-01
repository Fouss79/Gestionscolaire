package com.saas.school.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RapportPaiementDTO {

    // ===============================
    // INFORMATIONS ÉLÈVE
    // ===============================

    private String nomEleve;

    private String matricule;

    private String classe;

    private String anneeScolaire;


    // ===============================
    // PAIEMENTS
    // ===============================

    private List<LignePaiementDTO> paiements = new ArrayList<>();


    // ===============================
    // RÉSUMÉ
    // ===============================

    private double totalAPayer;

    private double totalPaye;

    private double resteAPayer;

    private double pourcentagePaye;


    // ===============================
    // LIGNE PAIEMENT
    // ===============================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LignePaiementDTO {

        private String reference;

        private String date;

        private String typeFrais;

        private String periode;

        private double montant;

        private String modePaiement;
    }
}