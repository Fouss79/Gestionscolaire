package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "composition_epreuve",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_composition_epreuve_inscription",
                        columnNames = {"epreuve_id", "inscription_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompositionEpreuve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "epreuve_id", nullable = false)
    private EpreuveExamen epreuve;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "statut",
            nullable = false,
            length = 20
    )
    @Builder.Default
    private StatutComposition statut = StatutComposition.NON_CONFIRME;
}