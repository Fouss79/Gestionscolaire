
        package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class RecetteRequestDTO {

    private Double montant;
    private String libelle;
    private String reference;
    private String modePaiement;

    private LocalDate dateRecette;

    // Année scolaire concernée par la recette
    private Long anneeScolaireId;
}

