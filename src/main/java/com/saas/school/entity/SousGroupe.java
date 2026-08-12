package com.saas.school.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"classe_id", "nom", "annee_scolaire_id"})
})
public class SousGroupe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    @Enumerated(EnumType.STRING)
    private TypeSousGroupe type;

    @JsonIgnoreProperties({"sousGroupes"})
    @ManyToOne
    @JoinColumn(name = "classe_id", nullable = false)
    private Classe classe;

    private Integer effectifMax;

    // ✅ AJOUT : Année scolaire
    @ManyToOne
    @JoinColumn(name = "annee_scolaire_id", nullable = false)
    private AnneeScolaire anneeScolaire;

    public enum TypeSousGroupe {
        TP, LANGUE, NIVEAU, AUTRE
    }
}