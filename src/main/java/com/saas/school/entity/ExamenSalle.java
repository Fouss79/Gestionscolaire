package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "examen_salle",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_examen_salle",
                        columnNames = {"examen_id", "salle_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ExamenSalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id", nullable = false)
    private Examen examen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;
}