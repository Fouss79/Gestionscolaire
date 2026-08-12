package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"inscription_id", "coefficient_matiere_id", "periode", "sous_groupe_id"})
})
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String periode;
    private Double nClass;
    private Double nExem;
    private Integer coeff;

    @ManyToOne
    private Eleve eleve;

    @ManyToOne
    private Matiere matiere;

    @ManyToOne
    private Classe classe;

    @ManyToOne
    private AnneeScolaire anneeScolaire;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inscription_id")
    private Inscription inscription;

    @ManyToOne
    @JoinColumn(name = "coefficient_matiere_id", nullable = false)
    private CoefficientMatiere coefficientMatiere;
    @ManyToOne
    @JoinColumn(name = "sous_groupe_id")
    private SousGroupe sousGroupe; // null = note de classe entière, sinon note du sous-groupe (ex: LV2)

}