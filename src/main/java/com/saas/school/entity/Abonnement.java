package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
public class Abonnement {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private PlanAbonnement plan;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private boolean actif;

    @ManyToOne
    private Ecole ecole;
}