package com.saas.school.dto;


import com.saas.school.entity.DecisionConseil;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Toutes les valeurs à injecter dans le document "Résultats de fin d'année".
 *
 * Adapte le package (com.saas.school.resultats) à ton arborescence réelle
 * si ton package racine n'est pas exactement com.saas.school.
 *
 * Assemble ce DTO à partir de tes entités existantes (Inscription, Eleve,
 * Classe, AnneeScolaire, Notes) dans le contrôleur.
 */
@Getter
@Setter
public class ResultatFinAnneeDto {

    // --- Identité de l'élève ---
    private String nomEtPrenom;
    private String matricule;

    // --- Classe ---
    private String classeNom;
    private int effectifClasse;

    // --- Année scolaire (ex : 2025 / 2026) ---
    private String anneeDebut;
    private String anneeFin;

    // --- Résultats ---
    private Double moyenneAnnuelle; // sur 10 (primaire) ou sur 20 (secondaire)
    private Integer rangDansClasse; // optionnel, peut rester null

    // --- Décision du conseil des maîtres ---
    private DecisionConseil decision;

    // --- Pied de page ---
    private String lieu = "Bamako";
    private LocalDate dateEdition = LocalDate.now();

    /**
     * Utilisé dans le template pour savoir quelle case cocher dans le
     * tableau "DECISION DU CONSEIL DES MAITRES".
     */
    public boolean isDecisionCochee(DecisionConseil option) {
        return decision == option;
    }

    public String getMoyenneFormatee() {
        return moyenneAnnuelle != null
                ? String.format(Locale.FRENCH, "%.2f", moyenneAnnuelle)
                : "-";
    }

    public String getRangFormate() {
        return rangDansClasse != null
                ? rangDansClasse + "e / " + effectifClasse
                : "-";
    }

    public String getDateFormatee() {
        if (dateEdition == null) {
            return "";
        }
        return dateEdition.getDayOfMonth() + " "
                + dateEdition.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " "
                + dateEdition.getYear();
    }
}