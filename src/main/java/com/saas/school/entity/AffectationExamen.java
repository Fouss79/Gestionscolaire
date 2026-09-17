package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(
        name = "affectation_examen",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "examen_id",
                                "inscription_id"
                        }
                ),
                @UniqueConstraint(
                        columnNames = {
                                "examen_id",
                                "salle_id",
                                "numero_place"
                        }
                )
        }
)
public class AffectationExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "examen_id",
            nullable = false
    )
    private Examen examen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "inscription_id",
            nullable = false
    )
    private Inscription inscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "salle_id",
            nullable = false
    )
    private Salle salle;

    @Column(
            name = "numero_place",
            nullable = false
    )
    private Integer numeroPlace;
}