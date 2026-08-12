package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InscriptionDTO {

    // --- Identité élève ---
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String sexe;
    private String lieuNaissance;
    private String nationalite;
    private String numeroExtraitNaissance;
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
    private String professionTuteur;
    private String adresseTuteur;

    // --- Scolarité ---
    private Long classeId;
    private Long anneeScolaireId;
    private Long ecoleId;
    private String ecoleProvenance;
}