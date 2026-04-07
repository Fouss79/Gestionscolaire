package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatiereClasse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Matiere matiere;

    @ManyToOne
    private Classe classe;

    private int coefficient;
    private int nombreHeures; // ex: Maths = 5h

    @ManyToOne
    private AnneeScolaire anneeScolaire;

    @ManyToOne
    private Ecole ecole;
}
