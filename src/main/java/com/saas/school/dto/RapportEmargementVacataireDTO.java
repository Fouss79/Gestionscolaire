package com.saas.school.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RapportEmargementVacataireDTO {

    private Long enseignantId;
    private String nomEnseignant;
    private String matricule;
    private String mois;
    private String tauxHoraire;

    private List<LigneEmargementVacataireDTO> lignes;

    private int nombreSeances;
    private int totalMinutes;
    private double totalMontant;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LigneEmargementVacataireDTO {

        private String date;
        private String jour;
        private String classe;
        private String matiere;

        private String heureDebut;
        private String heureFin;

        private int dureeMinutes;

        private double tauxHoraire;
        private double montant;

        private boolean present;
    }
}