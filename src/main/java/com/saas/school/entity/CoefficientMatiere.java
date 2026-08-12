package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "coefficient_matiere",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coefficient_matiere",
                        columnNames = {
                                "ecole_id",
                                "matiere_id",
                                "niveau_id",
                                "serie_id",
                                "annee_scolaire_id",
                                "classe_id",
                                "sous_groupe_id"
                        }
                )
        }
)
public class CoefficientMatiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "matiere_id", nullable = false)
    private Matiere matiere;

    @ManyToOne
    @JoinColumn(name = "niveau_id", nullable = false)
    private Niveau niveau;

    @ManyToOne
    @JoinColumn(name = "serie_id")
    private Serie serie; // null = s'applique à toutes les séries de ce niveau (ex: collège sans série)

    @Column(nullable = false)
    private Integer coefficient;
    private Integer nombreHeuresParSemaine;

    @ManyToOne
    @JoinColumn(name = "ecole_id", nullable = false)
    private Ecole ecole;
    @ManyToOne
    @JoinColumn(name = "annee_scolaire_id", nullable = false)
    private AnneeScolaire anneeScolaire;
    @ManyToOne
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @ManyToOne
    @JoinColumn(name = "sous_groupe_id")
    private SousGroupe sousGroupe;

}