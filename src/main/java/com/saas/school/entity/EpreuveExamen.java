package com.saas.school.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(
        name = "epreuve_examen",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_epreuve_examen_coefficient",
                columnNames = {"examen_id", "coefficient_matiere_id"}
        )
)
public class EpreuveExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examen_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Examen examen;

    // Porte la matière, le coefficient, le niveau et la série (programme).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coefficient_matiere_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CoefficientMatiere coefficientMatiere;

    // Nullable tant que l'épreuve n'est pas programmée sur un créneau.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creneau_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CreneauExamen creneau;

    // Optionnel : si non renseigné, dérivé du créneau (heureFin - heureDebut).
    private Integer dureeMinutes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}