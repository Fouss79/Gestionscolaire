package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"enseignant_id", "classe_id", "coefficient_matiere_id"})
})
public class AffectationEnseignant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "enseignant_id", nullable = false)
    private Enseignant enseignant;

    @ManyToOne
    @JoinColumn(name = "classe_id", nullable = false)
    private Classe classe;

    // 🔥 Porte déjà matière + niveau + série + année + coefficient + heures/semaine
    @ManyToOne
    @JoinColumn(name = "coefficient_matiere_id", nullable = false)
    private CoefficientMatiere coefficientMatiere;
}