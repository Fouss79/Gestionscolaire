package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "personnels")
public class Personnel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // ==========================
    // IDENTITE
    // ==========================

    private String nom;

    private String prenom;

    private LocalDate dateNaissance;

    private String lieuNaissance;

    private String sexe;

    private String nationalite;


    // ==========================
    // CONTACT
    // ==========================

    private String telephone;

    private String telephoneSecondaire;

    private String email;

    private String adresse;


    // ==========================
    // CONTACT URGENCE
    // ==========================

    private String contactUrgenceNom;

    private String contactUrgenceTelephone;



    // ==========================
    // INFORMATIONS PROFESSIONNELLES
    // ==========================

    @Column(unique = true)
    private String matricule;


    private LocalDate dateEmbauche;

    private LocalDate dateFinContrat;


    private Double salaireBase;


    private Boolean actif = true;



    // ==========================
    // AUDIT
    // ==========================

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;



    // ==========================
    // RELATIONS
    // ==========================


    /**
     * Chaque personnel appartient à une école
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecole_id")
    private Ecole ecole;



    /**
     * Le personnel possède un compte utilisateur
     * Le rôle est dans Utilisateur
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;



    @PrePersist
    public void prePersist(){

        createdAt = LocalDateTime.now();

        if(actif == null){
            actif = true;
        }

    }


    @PreUpdate
    public void preUpdate(){

        updatedAt = LocalDateTime.now();

    }

}