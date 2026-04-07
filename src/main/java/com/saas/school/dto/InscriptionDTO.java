package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class InscriptionDTO {

    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String sexe;

    private Long classeId;
    private Long anneeScolaireId;
    private Long ecoleId; // 🔥 ajouté
}