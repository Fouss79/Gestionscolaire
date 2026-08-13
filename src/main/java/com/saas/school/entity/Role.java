package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Table(
        name = "role",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_role_nom_ecole",
                columnNames = {"nom", "ecole_id"}
        )
)
@Data
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom; // ex: ADMIN, PROF, ELEVE

    @ManyToOne
    @JoinColumn(name = "ecole_id")
    private Ecole ecole;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private List<Permission> permissions;
}