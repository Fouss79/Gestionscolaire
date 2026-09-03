package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EnseignantResponseDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String matricule;
    private LocalDate dateNaissance;
    private String sexe;
    private String telephone;
    private String email;
    private String specialite;
    private String niveauDiplome;
    private String typeContrat;
    private Double salaireBase;
    private Double tauxHoraire;
    private Boolean actif;

    private List<Long> matiereIds;
    private List<String> matiereNoms;
}