package com.saas.school.service;

import com.saas.school.entity.CoefficientMatiere;
import com.saas.school.entity.Inscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgrammeEleveService {

    /**
     * Vérifie si une inscription est compatible avec un programme.
     *
     * Règles :
     * - Le programme sans classe est compatible avec toutes les classes.
     * - Sinon l'inscription doit appartenir à la classe du programme.
     * - Sans sous-groupe : tous les élèves de la classe sont concernés.
     * - Avec sous-groupe : l'élève doit appartenir à ce sous-groupe.
     */
    public boolean estCompatible(
            Inscription inscription,
            CoefficientMatiere programme
    ) {
        if (inscription == null || programme == null) {
            return false;
        }

        // Vérification de la classe
        if (programme.getClasse() != null) {

            if (inscription.getClasse() == null) {
                return false;
            }

            if (!programme.getClasse().getId()
                    .equals(inscription.getClasse().getId())) {
                return false;
            }
        }

        // Pas de sous-groupe :
        // tous les élèves compatibles avec la classe
        if (programme.getSousGroupe() == null) {
            return true;
        }

        // Le programme possède un sous-groupe :
        // l'élève doit appartenir à ce sous-groupe
        if (inscription.getEleve() == null
                || inscription.getEleve().getSousGroupes() == null
                || inscription.getEleve().getSousGroupes().isEmpty()) {
            return false;
        }

        Long sousGroupeId = programme.getSousGroupe().getId();

        return inscription.getEleve()
                .getSousGroupes()
                .stream()
                .anyMatch(sg ->
                        sg != null
                                && sg.getId() != null
                                && sg.getId().equals(sousGroupeId)
                );
    }

    /**
     * Filtre les inscriptions compatibles avec le programme.
     */
    public List<Inscription> filtrerInscriptions(
            List<Inscription> inscriptions,
            CoefficientMatiere programme
    ) {
        if (inscriptions == null || inscriptions.isEmpty()) {
            return List.of();
        }

        return inscriptions.stream()
                .filter(inscription ->
                        estCompatible(inscription, programme)
                )
                .toList();
    }
}