package com.saas.school.service;

import com.saas.school.dto.CompositionEpreuveRequest;
import com.saas.school.dto.CompositionEpreuveResponse;
import com.saas.school.entity.*;
import com.saas.school.exception.ExamenBusinessException;
import com.saas.school.repository.CompositionEpreuveRepository;
import com.saas.school.repository.EpreuveExamenRepository;
import com.saas.school.repository.InscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompositionEpreuveService {

    private final CompositionEpreuveRepository repository;
    private final EpreuveExamenRepository epreuveRepository;
    private final InscriptionRepository inscriptionRepository;

    /**
     * Récupère les compositions d'une épreuve.
     *
     * Si une composition n'existe pas encore pour un élève,
     * elle sera créée avec le statut NON_CONFIRME.
     */
    @Transactional
    public List<CompositionEpreuveResponse> getCompositions(
            Long epreuveId
    ) {

        EpreuveExamen epreuve = getEpreuve(epreuveId);

        List<CompositionEpreuve> existantes =
                repository.findByEpreuveId(epreuveId);

        return existantes.stream()
                .map(CompositionEpreuveResponse::from)
                .toList();
    }

    /**
     * Confirme le statut d'un élève pour une épreuve.
     */
    @Transactional
    public CompositionEpreuveResponse modifierStatut(
            Long epreuveId,
            Long inscriptionId,
            CompositionEpreuveRequest request
    ) {

        EpreuveExamen epreuve = getEpreuve(epreuveId);

        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() ->
                        new ExamenBusinessException(
                                "Inscription introuvable"
                        )
                );

        verifierEleveConcerneParEpreuve(epreuve, inscription);

        CompositionEpreuve composition =
                repository.findByEpreuveIdAndInscriptionId(
                        epreuveId,
                        inscriptionId
                ).orElseGet(() -> {
                    CompositionEpreuve nouvelle =
                            new CompositionEpreuve();

                    nouvelle.setEpreuve(epreuve);
                    nouvelle.setInscription(inscription);
                    nouvelle.setStatut(
                            StatutComposition.NON_CONFIRME
                    );

                    return nouvelle;
                });

        composition.setStatut(request.getStatut());

        CompositionEpreuve sauvegardee =
                repository.save(composition);

        return CompositionEpreuveResponse.from(sauvegardee);
    }

    /**
     * Remet un élève à NON_CONFIRME.
     */
    @Transactional
    public CompositionEpreuveResponse remettreNonConfirme(
            Long epreuveId,
            Long inscriptionId
    ) {

        CompositionEpreuveRequest request =
                new CompositionEpreuveRequest(
                        StatutComposition.NON_CONFIRME
                );

        return modifierStatut(
                epreuveId,
                inscriptionId,
                request
        );
    }

    private void verifierEleveConcerneParEpreuve(
            EpreuveExamen epreuve,
            Inscription inscription
    ) {

        if (epreuve.getExamen() == null) {
            throw new ExamenBusinessException(
                    "L'épreuve n'est associée à aucun examen"
            );
        }

        Long examenId = epreuve.getExamen().getId();

        if (inscription.getClasse() == null) {
            throw new ExamenBusinessException(
                    "L'inscription n'est associée à aucune classe"
            );
        }

        boolean classeConcernee =
                epreuve.getExamen()
                        .getClasses()
                        .stream()
                        .anyMatch(classe ->
                                classe.getId().equals(
                                        inscription.getClasse().getId()
                                )
                        );

        if (!classeConcernee) {
            throw new ExamenBusinessException(
                    "Cet élève ne fait pas partie des classes de cet examen"
            );
        }

        if (inscription.getAnneeScolaire() == null ||
                epreuve.getExamen().getAnneeScolaire() == null ||
                !inscription.getAnneeScolaire().getId()
                        .equals(epreuve.getExamen().getAnneeScolaire().getId())) {

            throw new ExamenBusinessException(
                    "L'élève n'appartient pas à l'année scolaire de cet examen"
            );
        }
    }

    private EpreuveExamen getEpreuve(Long id) {

        return epreuveRepository.findById(id)
                .orElseThrow(() ->
                        new ExamenBusinessException(
                                "Épreuve introuvable"
                        )
                );
    }
}