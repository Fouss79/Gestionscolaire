package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class PaiementDepense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ============================================================
    // DÉPENSE
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depense_id", nullable = false)
    private Depense depense;


    // ============================================================
    // ANNÉE SCOLAIRE
    // ============================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "annee_scolaire_id", nullable = false)
    private AnneeScolaire anneeScolaire;


    // ============================================================
    // PAIEMENT
    // ============================================================

    @Column(nullable = false)
    private Double montant;

    @Column(nullable = false)
    private String modePaiement;

    private String reference;

    @Column(nullable = false)
    private LocalDateTime datePaiement;
}