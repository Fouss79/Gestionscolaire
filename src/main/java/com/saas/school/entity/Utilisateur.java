package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private String email;

    private String password;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
    @ManyToOne
    @JoinColumn(name = "ecole_id")
    private Ecole ecole;
    private String motDePasseTemporaire;
}