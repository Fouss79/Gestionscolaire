package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ecole_id", "annee_scolaire_id", "nom"})
})
public class Periode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private Integer ordre;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    @ManyToOne
    @JoinColumn(name = "annee_scolaire_id", nullable = false)
    private AnneeScolaire anneeScolaire;

    @ManyToOne
    @JoinColumn(name = "ecole_id", nullable = false)
    private Ecole ecole;
}