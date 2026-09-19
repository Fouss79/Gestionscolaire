package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(
        name = "epreuve_salle",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_epreuve_salle",
                columnNames = {"epreuve_id", "salle_id"}
        )
)
public class EpreuveSalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "epreuve_id", nullable = false)
    private EpreuveExamen epreuve;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;
}