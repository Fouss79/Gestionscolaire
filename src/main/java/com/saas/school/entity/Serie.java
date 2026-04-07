package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Serie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom; // A, D, C, Technique...

    @ManyToOne
    @JoinColumn(name = "ecole_id")
    private Ecole ecole;
}