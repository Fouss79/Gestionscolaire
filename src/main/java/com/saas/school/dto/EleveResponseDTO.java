package com.saas.school.dto;

import lombok.Data;

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
}
