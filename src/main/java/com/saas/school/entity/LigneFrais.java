package com.saas.school.entity;

import com.saas.school.service.StatutPaiement;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"inscription_id", "type_frais_id", "mois", "annee"})
})
public class LigneFrais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @ManyToOne
    @JoinColumn(name = "type_frais_id", nullable = false)
    private TypeFrais typeFrais;
    private Integer mois;   // null pour une ligne de frais annuelle
    private Integer annee;  // null ou année scolaire selon ton modèle
    @Column(nullable = false)
    private Double montantTotal;

    @Column(nullable = false)
    private Double montantPaye = 0.0;

    @Column(nullable = false)
    private Double resteAPayer;

    @Enumerated(EnumType.STRING)
    private StatutPaiement statutPaiement;

    @Column(nullable = false)
    private boolean estimatif = false; // true = montant par défaut, tarif non encore configuré par l'admin
}