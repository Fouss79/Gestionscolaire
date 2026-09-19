package com.saas.school.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(
        name = "epreuve_enseignant",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_epreuve_enseignant_role",
                columnNames = {"epreuve_id", "enseignant_id", "role"}
        )
)
public class EpreuveEnseignant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epreuve_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private EpreuveExamen epreuve;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enseignant_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Enseignant enseignant;

    @Enumerated(EnumType.STRING)
    private RoleEpreuve role; // nullable, structure extensible

    public enum RoleEpreuve {
        SURVEILLANT, CORRECTEUR, RESPONSABLE
    }
}