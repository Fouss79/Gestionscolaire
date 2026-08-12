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

    private int heureDebut;

    private int heureFin;

    @ManyToOne
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @ManyToOne
    @JoinColumn(name = "matiere_id")
    private Matiere matiere;

    @ManyToOne
    @JoinColumn(name = "enseignant_id")
    private Enseignant enseignant;

    @ManyToOne
    @JoinColumn(name = "annee_scolaire_id")
    private AnneeScolaire anneeScolaire;

    @ManyToOne
    @JoinColumn(name = "salle_id")
    private Salle salle;

    // 🔥 Sous-groupe concerné par le cours
    // null = cours pour toute la classe
    @ManyToOne
    @JoinColumn(name = "sous_groupe_id")
    private SousGroupe sousGroupe;
}