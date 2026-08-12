package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PersonnelRequest {

    private String nom;
    private String prenom;

    private LocalDate dateNaissance;
    private String lieuNaissance;
    private String sexe;
    private String nationalite;

    private String telephone;
    private String telephoneSecondaire;

    private String email;
    private String adresse;

    private String contactUrgenceNom;
    private String contactUrgenceTelephone;

    private LocalDate dateEmbauche;
    private LocalDate dateFinContrat;

    private String role; // ENSEIGNANT, COMPTABLE...

    private Long ecoleId;

}