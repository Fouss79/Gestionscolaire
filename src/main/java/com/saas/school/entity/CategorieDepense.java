package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(
        name = "categories_depenses",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"ecole_id", "nom"}
                )
        }
)
@Data
public class CategorieDepense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecole_id", nullable = false)
    private Ecole ecole;
}
