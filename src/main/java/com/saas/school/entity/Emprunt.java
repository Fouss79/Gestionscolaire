package com.saas.school.entity;

import com.saas.school.service.StatutPaiement;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "emprunts")
public class Emprunt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String preteur;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * École qui contracte l'emprunt
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecole_id", nullable = false)
    private Ecole ecole;

    /**
     * Libellé de l'emprunt
     * Exemple : Prêt bancaire pour rénovation
     */
    @Column(nullable = false)
    private String libelle;

    /**
     * Montant réellement emprunté
     */
    @Column(nullable = false)
    private Double montantEmprunte;

    /**
     * Montant total à rembourser.
     *
     * Peut être supérieur au montant emprunté
     * si l'emprunt comporte des intérêts/frais.
     */
    @Column(nullable = false)
    private Double montantARembourser;

    /**
     * Total déjà remboursé
     */
    @Column(nullable = false)
    private Double montantRembourse = 0.0;

    /**
     * Montant restant à payer
     */
    @Column(nullable = false)
    private Double resteAPayer;

    /**
     * Date de création de l'emprunt
     */
    @Column(nullable = false)
    private LocalDateTime dateEmprunt;

    /**
     * Date prévue de fin de remboursement
     */
    private LocalDateTime dateEcheance;

    /**
     * Statut du remboursement
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPaiement statutPaiement = StatutPaiement.NON_PAYE;

    /**
     * Les différents remboursements effectués
     */
    @OneToMany(
            mappedBy = "emprunt",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<RemboursementEmprunt> remboursements = new ArrayList<>();

    @PrePersist
    public void prePersist() {

        if (dateEmprunt == null) {
            dateEmprunt = LocalDateTime.now();
        }

        if (montantRembourse == null) {
            montantRembourse = 0.0;
        }

        if (montantARembourser != null && resteAPayer == null) {
            resteAPayer = montantARembourser;
        }

        if (statutPaiement == null) {
            statutPaiement = StatutPaiement.NON_PAYE;
        }
    }
}