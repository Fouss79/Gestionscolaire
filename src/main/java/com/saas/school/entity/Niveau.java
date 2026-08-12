package com.saas.school.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Niveau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom; // 6ème, 5ème, Terminale...
    @ManyToOne
    @JoinColumn(name = "ecole_id")
    private Ecole ecole;
    @ManyToOne
    @JoinColumn(name = "cycle_id")
    private Cycle cycle; // optionnel, pour ne pas casser les niveaux existants sans cycle
}