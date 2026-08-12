package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class Emargement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jour;

    private LocalDate dateHeure;

    private int duree;

    private boolean present;

    // ✅ RELATIONS OBLIGATOIRES

    @ManyToOne
    @JoinColumn(name = "enseignant_id")
    private Enseignant enseignant;

    @ManyToOne
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @ManyToOne
    @JoinColumn(name = "matiere_id")
    private Matiere matiere;

    @ManyToOne
    @JoinColumn(name = "annee_id")
    private AnneeScolaire anneeScolaire;
    @ManyToOne
    private EmploiDuTemps emploiDuTemps;

}