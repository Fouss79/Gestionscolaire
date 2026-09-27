package com.saas.school.dto;

import com.saas.school.entity.DecisionConseil;

public record ReinscriptionPrimaireDto(
        Long ancienneInscriptionId,
        Long eleveId,
        String nomEtPrenom,
        String matricule,
        String ancienneClasse,
        Double moyenneAnnuelle,
        Integer rang,
        DecisionConseil decisionConseil,
        String statutReinscription,
        Long nouvelleInscriptionId,
        Long nouvelleClasseId,
        String nouvelleClasse
) {}