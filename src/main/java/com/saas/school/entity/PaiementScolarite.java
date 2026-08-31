package com.saas.school.entity;

import com.saas.school.entity.LigneFrais;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "paiement_scolarite")
public class PaiementScolarite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_frais_id", nullable = false)
    private LigneFrais ligneFrais;

    @Column(nullable = false)
    private Double montant;

    @Column(length = 50)
    private String modePaiement;

    @Column(unique = true, length = 100)
    private String reference;

    @Column(nullable = false)
    private LocalDateTime datePaiement;

    // Mois concerné par le paiement
    // 1 = janvier ... 12 = décembre
    private Integer mois;

    // Année civile du paiement
    private Integer annee;

    @PrePersist
    public void prePersist() {
        if (datePaiement == null) {
            datePaiement = LocalDateTime.now();
        }
    }
}