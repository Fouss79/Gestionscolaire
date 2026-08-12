package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Cycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom; // "Primaire", "Collège", "Lycée"

    @ManyToOne
    @JoinColumn(name = "ecole_id", nullable = false)
    private Ecole ecole;
}