package com.saas.school.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "eleve", uniqueConstraints = {
        @UniqueConstraint(columnNames = "matricule")
})
public class Eleve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------- Identité de l'élève ----------
    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private LocalDate dateNaissance;

    private String lieuNaissance;

    private String nationalite;

    @Column(unique = true, nullable = false)
    private String matricule;

    private String sexe; // enum plus fiable qu'un String libre

    private String numeroExtraitNaissance; // n° acte/extrait de naissance

    private String groupeSanguin;

    @Column(length = 500)
    private String allergiesMaladies; // infos médicales utiles

    @Column(length = 255)
    private String photoUrl; // photo d'identité de l'élève

    // ---------- Coordonnées ----------
    private String adresse;

    private String telephone; // si l'élève en a un (secondaire/lycée)

    private String email;

    // ---------- Parent / Tuteur ----------
    private String nomTuteur;

    private String prenomTuteur;

    private String lienParente; // Père, Mère, Tuteur légal...

    private String telephoneTuteur;

    private String emailTuteur;

    private String professionTuteur;

    private String adresseTuteur;

    // ---------- Scolarité ----------
    @ManyToOne
    @JoinColumn(name = "classe_id")
    private Classe classe;

    @ManyToOne
    @JoinColumn(name = "ecole_id")
    private Ecole ecole;

    private String anneeScolaire; // ex: "2025-2026"

    private LocalDate dateInscription; // date réelle d'inscription

    @Enumerated(EnumType.STRING)
    private StatutEleve statut; // NOUVEAU, REDOUBLANT, TRANSFERE, ...

    private String ecoleProvenance; // école précédente si transfert

    @Column(length = 500)
    private String documentsJustificatifs; // liste/chemin des pièces fournies

    // ---------- Compte utilisateur lié ----------
    @OneToOne
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    // ---------- Suivi administratif ----------
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean active = true; // renommé (majuscule = bug Lombok)

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---------- Enums internes ----------
    public enum Sexe {
        M, F
    }

    public enum StatutEleve {
        NOUVEAU, REDOUBLANT, TRANSFERE, DIPLOME, ABANDON
    }
    @ManyToMany
    @JoinTable(
            name = "eleve_sous_groupe",
            joinColumns = @JoinColumn(name = "eleve_id"),
            inverseJoinColumns = @JoinColumn(name = "sous_groupe_id")
    )
    @JsonIgnoreProperties({"eleves"})
    private Set<SousGroupe> sousGroupes = new HashSet<>();

}