package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class EmploiDuTemps {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jour;
    private String heure;

    @ManyToOne
    private Classe classe;

    @ManyToOne
    private Matiere matiere;

    @ManyToOne
    private Enseignant enseignant;

    @ManyToOne
    private AnneeScolaire anneeScolaire;

    private int heureDebut;
    private int heureFin;
}
