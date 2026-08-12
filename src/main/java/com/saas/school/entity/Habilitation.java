package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Habilitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Enseignant enseignant;

    @ManyToOne
    private Matiere matiere;

    @ManyToOne
    private Ecole ecole;
}