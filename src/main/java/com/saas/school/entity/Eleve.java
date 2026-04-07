package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
public class Eleve {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String matricule;
    private String sexe;
    @ManyToOne
    @JoinColumn(name = "classe_id")
    private Classe classe;
    @ManyToOne
    @JoinColumn(name = "ecole_id")
    private Ecole ecole;
    private LocalDateTime createdAt;
    private boolean Active;
}