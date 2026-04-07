package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InscriptionResponseDTO {

    private Long id;

    private String nom;
    private String prenom;
    private String matricule;

    private String classeNom;
    private String annee;

    private LocalDateTime dateInscription;
    private String statut;
}
