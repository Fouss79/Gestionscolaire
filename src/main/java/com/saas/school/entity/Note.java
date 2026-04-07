package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String periode;
    private Double nClass;
    private Double nExem;
    private Double coeff;

    @ManyToOne
    private Eleve eleve;

    @ManyToOne
    private Matiere matiere;

    @ManyToOne
    private Classe classe;

    @ManyToOne
    private AnneeScolaire anneeScolaire;
}