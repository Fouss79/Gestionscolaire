package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(uniqueConstraints = {
        // Un seul enregistrement de présence par élève, par jour, par créneau (matière/heure)
        @UniqueConstraint(columnNames = {"inscription_id", "date", "emploi_du_temps_id"})
})
public class Presence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inscription_id", nullable = false)
    private Inscription inscription;

    @Column(nullable = false)
    private LocalDate date;

    // Optionnel — si tu émarges par créneau précis (cours de Maths de 8h à 10h),
    // sinon laisse null pour une présence "journée entière"
    @ManyToOne
    @JoinColumn(name = "emploi_du_temps_id")
    private EmploiDuTemps emploiDuTemps;

    // 🔥 Rattachement à la période (trimestre) — sert uniquement au filtrage/rapports
    @ManyToOne
    @JoinColumn(name = "periode_id")
    private Periode periode;

    @Enumerated(EnumType.STRING)
    private StatutPresence statut;

    private String motif; // "Maladie", "Autorisation parentale"... optionnel

    @ManyToOne
    @JoinColumn(name = "saisi_par_id")
    private Utilisateur saisiPar;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum StatutPresence {
        PRESENT, ABSENT, RETARD, EXCUSE
    }
}