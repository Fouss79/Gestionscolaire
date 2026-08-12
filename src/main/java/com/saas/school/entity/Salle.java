package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom; // "Salle 12", "Labo Physique", "Amphithéâtre"

    private Integer capacite;

    @ManyToOne
    @JoinColumn(name = "ecole_id", nullable = false)
    private Ecole ecole;
}