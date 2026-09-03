package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EnseignantRequest {

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

    private String specialite;
    private String niveauDiplome;
    private String diplomeObtenu;

    private String typeContrat;
    private LocalDate dateEmbauche;
    private LocalDate dateFinContrat;
    private Double salaireBase;
    private Double tauxHoraire;
    private Integer nombreHeuresParSemaine;

    private List<Long> matiereIds;

    private Long ecoleId;
}