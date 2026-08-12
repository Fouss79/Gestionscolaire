package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Tarif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double montant;

    @ManyToOne
    private TypeFrais typeFrais;

    @ManyToOne
    private Niveau niveau; // ⚠️ remplace l'ancien champ "classe"

    @ManyToOne
    private AnneeScolaire anneeScolaire;

    @ManyToOne
    private Ecole ecole;

    public Long getId() {
        return id;
    }

    public void setEcole(Ecole ecole) {
        this.ecole = ecole;
    }

    public Ecole getEcole() {
        return ecole;
    }

    public void setNiveau(Niveau niveau) {
        this.niveau = niveau;
    }

    public Niveau getNiveau() {
        return niveau;
    }

    public void setAnneeScolaire(AnneeScolaire annee) {
        this.anneeScolaire = annee;
    }

    public AnneeScolaire getAnneeScolaire() {
        return anneeScolaire;
    }

    public void setTypeFrais(TypeFrais typeFrais) {
        this.typeFrais = typeFrais;
    }

    public TypeFrais getTypeFrais() {
        return typeFrais;
    }

    public void setMontant(Double montant) {
        this.montant = montant;
    }

    public Double getMontant() {
        return montant;
    }
}