package com.saas.school.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Toutes les données affichées sur une carte scolaire (recto).
 *
 * Le champ photoBase64 / logoBase64 contient déjà une data URI
 * complète ("data:image/...;base64,...") prête à être posée dans un
 * attribut src de balise <img>, ou null si aucune image disponible
 * (le template affiche alors des initiales à la place).
 */
@Getter
@Builder
public class CarteScolaireDto {

    private Long inscriptionId;

    // --- Élève ---
    private String nom;
    private String prenom;
    private String matricule;
    private String dateNaissanceFormatee;
    private String lieuNaissance;
    private String groupeSanguin;
    private String photoBase64;

    // --- Scolarité ---
    private String classeNom;
    private String anneeScolaireNom;
    private String validiteDebut;
    private String validiteFin;

    // --- École ---
    private String ecoleNom;
    private String ecoleAdresse;
    private String ecoleVille;
    private String ecoleTelephone;
    private String logoBase64;
    private String initiales;
    private String directeurNom;
    private String telephoneParent;

    public String getNomComplet() {
        return (nom != null ? nom : "") + " " + (prenom != null ? prenom : "");
    }

    public String getInitiales() {
        String i1 = prenom != null && !prenom.isBlank() ? prenom.substring(0, 1) : "";
        String i2 = nom != null && !nom.isBlank() ? nom.substring(0, 1) : "";
        return (i1 + i2).toUpperCase();
    }
}