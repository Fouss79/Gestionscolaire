package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "creneau_horaire")
public class CreneauHoraire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "configuration_id", nullable = false)
    private ConfigurationCreneaux configuration;

    @Column(nullable = false)
    private String jour;

    // Toutes les heures sont enregistrées en minutes.
    private int heureDebut;
    private int heureFin;

    private int ordre;
}