package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "repartition_epreuve",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_repartition_epreuve_inscription",
                        columnNames = {
                                "epreuve_id",
                                "inscription_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RepartitionEpreuve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "epreuve_id", nullable = false)
    private EpreuveExamen epreuve;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;
}