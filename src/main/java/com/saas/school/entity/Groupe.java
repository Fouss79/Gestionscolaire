package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Groupe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom; // 1, 2, 3 ou A, B

    @ManyToOne
    @JoinColumn(name = "ecole_id")
    private Ecole ecole;
}
