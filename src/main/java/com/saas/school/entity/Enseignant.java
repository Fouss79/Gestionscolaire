package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "enseignant", uniqueConstraints = {
        @UniqueConstraint(columnNames = "matricule")
})
public class Enseignant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------- Identité ----------
    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    private LocalDate dateNaissance;

    private String lieuNaissance;

    private String sexe;

    private String nationalite;

    @Column(unique = true)
    private String matricule;

    @Column(length = 255)
    private String photoUrl;

    // ---------- Coordonnées ----------
    private String telephone;

    private String telephoneSecondaire;

    private String email;

    private String adresse;

    // ---------- Contact d'urgence ----------
    private String contactUrgenceNom;

    private String contactUrgenceTelephone;

    // ---------- Profil professionnel ----------
    private String specialite; // matière principale enseignée

    @Enumerated(EnumType.STRING)
    private NiveauDiplome niveauDiplome; // BAC, LICENCE, MASTER, DOCTORAT...

    private String diplomeObtenu; // intitulé précis du diplôme

    @Enumerated(EnumType.STRING)
    private TypeContrat typeContrat; // CDI, CDD, VACATAIRE, STAGIAIRE

    private LocalDate dateEmbauche;

    private LocalDate dateFinContrat; // pertinent si CDD/vacataire

    private Double salaireBase;

    private Integer nombreHeuresParSemaine;

    // ---------- Rattachement ----------
    @ManyToOne
    @JoinColumn(name = "ecole_id")
    private Ecole ecole;

    @OneToOne
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    // ---------- Documents administratifs ----------
    @Column(length = 500)
    private String documentsJustificatifs; // CV, diplômes, casier judiciaire...

    // ---------- Suivi administratif ----------
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean actif = true;

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
    public enum NiveauDiplome {
        BEPC, BAC, LICENCE, MASTER, DOCTORAT, AUTRE
    }

    public enum TypeContrat {
        CDI, CDD, VACATAIRE, STAGIAIRE
    }
    @ManyToMany
    @JoinTable(
            name = "enseignant_matiere",
            joinColumns = @JoinColumn(name = "enseignant_id"),
            inverseJoinColumns = @JoinColumn(name = "matiere_id")
    )
    private Set<Matiere> matieresEnseignees = new HashSet<>();
}