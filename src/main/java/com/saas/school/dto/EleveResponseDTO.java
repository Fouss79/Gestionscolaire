package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EleveResponseDTO {

    private Long id;
    private String nom;
    private String prenom;
    private String sexe;

    private String numeroMatricule;
    private String classeNom;
    private String anneeScolaire;
    private String statut;
    private String dateInscription;

    private Long classeId;

    // --- Ajouts pour l'endpoint /api/eleves/classe/{id} ---
    private LocalDate dateNaissance;
    private String photoUrl;
    private List<Long> sousGroupeIds;
}