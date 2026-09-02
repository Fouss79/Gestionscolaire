package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "operation_comptable",
        indexes = {
                @Index(name = "idx_operation_ecole", columnList = "ecole_id"),
                @Index(name = "idx_operation_date", columnList = "date_operation"),
                @Index(name = "idx_operation_nature", columnList = "nature")
        }
)
public class OperationComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =====================================================
    // ÉCOLE
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ecole_id", nullable = false)
    private Ecole ecole;

    // =====================================================
    // NATURE
    // =====================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NatureOperation nature;

    // =====================================================
    // INFORMATIONS OPÉRATION
    // =====================================================

    @Column(nullable = false)
    private Double montant;

    @Column(nullable = false)
    private LocalDateTime dateOperation;

    @Column(length = 255)
    private String libelle;

    @Column(length = 100)
    private String reference;

    @Column(length = 50)
    private String modePaiement;

    // =====================================================
    // LIEN AVEC PAIEMENT SCOLARITÉ (recette)
    // =====================================================

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paiement_scolarite_id", unique = true)
    private PaiementScolarite paiementScolarite;

    // =====================================================
    // LIEN AVEC VERSEMENT SUR UNE DÉPENSE (dépense échelonnée)
    // =====================================================

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paiement_depense_id", unique = true)
    private PaiementDepense paiementDepense;

    // =====================================================
    // CATÉGORIE DE DÉPENSE
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_depense_id")
    private CategorieDepense categorieDepense;

    // =====================================================
    // CRÉATION
    // =====================================================

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {

        if (dateOperation == null) {
            dateOperation = LocalDateTime.now();
        }

        createdAt = LocalDateTime.now();
    }
}