package com.saas.school.entity;

/**
 * Décision prise par le conseil des maîtres en fin d'année pour un élève.
 * Adapte le package à ton arborescence réelle si besoin.
 */
public enum DecisionConseil {
    ADMISSION_CLASSE_SUPERIEURE("Admission en classe supérieure"),
    REDOUBLEMENT("Redoublement"),
    EXCLUSION("Exclusion");

    private final String libelle;

    DecisionConseil(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}