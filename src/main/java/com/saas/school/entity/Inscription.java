package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 élève
    @ManyToOne
    private Eleve eleve;

    // 🔗 classe
    @ManyToOne
    private Classe classe;

    // 🔗 école
    @ManyToOne
    @JoinColumn(name = "ecole_id")
    private Ecole ecole;
    @ManyToOne
    @JoinColumn(name = "annee_id")
    private AnneeScolaire anneeScolaire;
    private LocalDateTime createdAt;

    private boolean active;
}
