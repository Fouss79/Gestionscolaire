package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@Entity
public class Ecole {

    @Id
    @GeneratedValue
    private Long id;

    private String nom;
    private String codeEcole;

    // infos générales
    private String adresse;
    private String ville;
    private String pays;
    private String telephone;
    private String email;

    // identité visuelle
    private String logo;

    // gestion SaaS
    private PlanAbonnement plan;
    private LocalDate dateFin;
    private boolean active;

    // audit
    private LocalDateTime createdAt;
}