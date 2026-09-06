package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "paiement_enseignant")
public class PaiementEnseignant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "enseignant_id", nullable = false)
    private Enseignant enseignant;

    @Column(nullable = false)
    private LocalDate periodeDebut;

    @Column(nullable = false)
    private LocalDate periodeFin;

    @Column(nullable = false)
    private Integer totalHeures;

    @Column(nullable = false)
    private Double tauxHoraire;

    @Column(nullable = false)
    private Double montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPaiement statut;

    private LocalDate datePaiement;

    @Column(nullable = false)
    private Long anneeScolaireId; // scope école, même logique que EmploiDuTemps

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.statut == null) this.statut = StatutPaiement.EN_ATTENTE;
    }

    public enum StatutPaiement {
        EN_ATTENTE, PAYE



    }

    @ManyToMany
    @JoinTable(
            name = "paiement_enseignant_emargement",
            joinColumns = @JoinColumn(name = "paiement_id"),
            inverseJoinColumns = @JoinColumn(name = "emargement_id")
    )
    private List<Emargement> emargements = new ArrayList<>();


}