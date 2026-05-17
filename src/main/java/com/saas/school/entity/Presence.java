package com.saas.school.entity;

import com.saas.school.service.StatutPresence;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
public class Presence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Eleve eleve;

    @ManyToOne
    private EmploiDuTemps emploiDuTemps;

    @Enumerated(EnumType.STRING)
    private StatutPresence statut;

    private LocalDate date; // 🔥 IMPORTANT
}