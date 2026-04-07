package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class BesoinHeure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Classe classe;

    @ManyToOne
    private Matiere matiere;

    private int nombreHeures; // ex: Maths = 5h

    @ManyToOne
    private AnneeScolaire anneeScolaire;
}