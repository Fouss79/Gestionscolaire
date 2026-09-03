package com.saas.school.dto;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaiementEnseignantDTO {
    private Long id;
    private Long enseignantId;
    private String enseignantNom;
    private String enseignantPrenom;
    private LocalDate periodeDebut;
    private LocalDate periodeFin;
    private int totalHeures;
    private double tauxHoraire;
    private double montant;
    private String statut; // NON_GENERE / EN_ATTENTE / PAYE
    private LocalDate datePaiement;
}