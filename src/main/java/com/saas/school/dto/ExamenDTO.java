package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ExamenDTO {

    private Long id;

    private String libelle;

    private LocalDate dateExamen;

    private LocalTime heureDebut;

    private LocalTime heureFin;

    private boolean actif;

    private Long ecoleId;

    private Long anneeScolaireId;

    // Pratiques à afficher côté frontend sans requête supplémentaire.
    private String anneeScolaireNom;
}