package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ecole_id", "code"})
})
public class Matiere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom; // "Mathématiques", "Français", "Anglais"...

    private String code; // "MATH", "FR", "ANG"

    @ManyToOne
    @JoinColumn(name = "ecole_id", nullable = false)
    private Ecole ecole;

    @Column(nullable = false)
    private boolean active = true;
}