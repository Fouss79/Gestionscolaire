package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Affectation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Enseignant enseignant;

    @ManyToOne
    private Classe classe;

    @ManyToOne
    private Matiere matiere;

    @ManyToOne
    private AnneeScolaire anneeScolaire;
}