package com.saas.school.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReinscriptionReponseDTO {

    private Long id;

    private String nom;
    private String prenom;
    private String matricule;

    private String classeNom;

    private LocalDate dateNaissance;
    private String sexe;

    private String annee;

    private String statutPaiement;

    private LocalDateTime dateInscription;

    private String statut;

    private Double montantTotal;
    private Double montantPaye;
    private Double resteAPayer;
    private String nouvelleClasseNom;
    private Long ecoleId;
    private  Double moyenneAnnuel;

    // ================= AJOUT POUR REINSCRIPTION =================

    private Double moyenneAnnuelle;

    private String mention;

    private String decision; // ADMIS / REDOUBLANT

    private String statutReinscription; // REINSCRIT / NON_REINSCRIT
}