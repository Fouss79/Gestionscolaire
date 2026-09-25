package com.saas.school.dto;

import java.math.BigDecimal;
import java.util.List;

public record BulletinDtos(
        Long inscriptionId,
        String eleve,
        String classe,
        String mois,
        Integer absences,
        List<LigneBulletinDto> lignes,
        BigDecimal total,
        BigDecimal moyenne,
        String observationMaitre,
        BigDecimal moyennePremier,
        Integer rang,
        Integer effectif,
        int noteMax
) {

    /*
     * Constructeur de compatibilité.
     *
     * Il permet de conserver temporairement les anciens appels
     * qui ne fournissent pas encore inscriptionId.
     */
    public BulletinDtos(
            String eleve,
            String classe,
            String mois,
            Integer absences,
            List<LigneBulletinDto> lignes,
            BigDecimal total,
            BigDecimal moyenne,
            String observationMaitre,
            BigDecimal moyennePremier,
            Integer rang,
            Integer effectif,
            int noteMax
    ) {
        this(
                null,
                eleve,
                classe,
                mois,
                absences,
                lignes,
                total,
                moyenne,
                observationMaitre,
                moyennePremier,
                rang,
                effectif,
                noteMax
        );
    }
}