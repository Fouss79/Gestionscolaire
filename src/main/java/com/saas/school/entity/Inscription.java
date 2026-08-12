package com.saas.school.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.saas.school.service.StatutInscription;
import com.saas.school.service.StatutPaiement;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eleve_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Eleve eleve;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classe_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Classe classe;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecole_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Ecole ecole;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annee_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private AnneeScolaire anneeScolaire;


    private LocalDateTime createdAt;


    @Enumerated(EnumType.STRING)
    private StatutInscription statut;


    @Enumerated(EnumType.STRING)
    private StatutPaiement statutPaiement;


    private Double montantTotal = 0.0;

    private Double montantPaye = 0.0;

    private Double resteAPayer = 0.0;
}