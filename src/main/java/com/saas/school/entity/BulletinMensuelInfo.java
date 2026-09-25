package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Informations propres au bulletin mensuel du premier cycle :
 * absences et observation du maître. Les notes restent dans la table Note existante.
 */
@Entity
@Table(name = "bulletin_mensuel_info",
       uniqueConstraints = @UniqueConstraint(name = "uk_bulletin_info_inscription_mois",
                                             columnNames = {"inscription_id", "mois"}))
@Getter
@Setter
@NoArgsConstructor
public class BulletinMensuelInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    /** Même valeur que « periode » des notes (ex. "Juin"). */
    @Column(nullable = false, length = 30)
    private String mois;

    @Column(nullable = false)
    private Integer absences = 0;

    @Column(name = "observation_maitre", length = 500)
    private String observationMaitre;
}
