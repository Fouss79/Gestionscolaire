package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class AnneeScolaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom; // ex: 2025-2026

    @ManyToOne
    @JoinColumn(name = "ecole_id")
    private Ecole ecole;

    private boolean active;

    private LocalDateTime createdAt;
}