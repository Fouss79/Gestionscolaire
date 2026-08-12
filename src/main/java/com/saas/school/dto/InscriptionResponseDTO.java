package com.saas.school.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class InscriptionResponseDTO {

    private Long id;

    // --- Identité élève ---
    private String nom;
    private String prenom;
    private String matricule;
    private LocalDate dateNaissance;
    private String sexe;
    private String lieuNaissance;
    private String nationalite;
    private String groupeSanguin;
    private String allergiesMaladies;

    // --- Coordonnées élève ---
    private String adresse;
    private String telephone;
    private String email;

    // --- Tuteur / Parent ---
    private String nomTuteur;
    private String prenomTuteur;
    private String lienParente;
    private String telephoneTuteur;
    private String emailTuteur;

    // --- Scolarité ---
    private String classeNom;
    private String annee;
    private LocalDateTime dateInscription;
    private String statut;
    private String statutPaiement;

    // --- Paiement ---
    private Double montantTotal;
    private Double montantPaye;
    private Double resteAPayer;

    private Long ecoleId;
}