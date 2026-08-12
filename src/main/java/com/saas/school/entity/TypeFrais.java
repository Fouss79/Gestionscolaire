package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class TypeFrais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false)
    private String code;


    @Column(nullable = false)
    private String libelle;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FrequenceFrais frequence;


    @ManyToOne
    @JoinColumn(name = "ecole_id", nullable = false)
    private Ecole ecole;

}