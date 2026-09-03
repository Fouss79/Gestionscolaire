package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "remboursements_emprunt")
public class RemboursementEmprunt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Emprunt concerné
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emprunt_id", nullable = false)
    private Emprunt emprunt;

    /**
     * Montant du remboursement
     */
    @Column(nullable = false)
    private Double montant;

    /**
     * Mode de paiement :
     * CASH, ORANGE_MONEY, WAVE, VIREMENT, etc.
     */
    @Column(nullable = false)
    private String modePaiement;

    /**
     * Référence du paiement.
     */
    @Column(nullable = false)
    private String reference;

    /**
     * Date du remboursement
     */
    @Column(nullable = false)
    private LocalDateTime dateRemboursement;

    @PrePersist
    public void prePersist() {
        if (dateRemboursement == null) {
            dateRemboursement = LocalDateTime.now();
        }
    }
}