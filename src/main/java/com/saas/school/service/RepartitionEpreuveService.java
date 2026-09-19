package com.saas.school.service;

import com.saas.school.dto.RepartitionEpreuveResponse;
import com.saas.school.entity.CoefficientMatiere;
import com.saas.school.entity.EpreuveExamen;
import com.saas.school.entity.EpreuveSalle;
import com.saas.school.entity.Inscription;
import com.saas.school.entity.RepartitionEpreuve;
import com.saas.school.entity.Salle;
import com.saas.school.exception.ExamenBusinessException;
import com.saas.school.repository.EpreuveExamenRepository;
import com.saas.school.repository.EpreuveSalleRepository;
import com.saas.school.repository.InscriptionRepository;
import com.saas.school.repository.RepartitionEpreuveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RepartitionEpreuveService {

    private final RepartitionEpreuveRepository repartitionRepository;
    private final EpreuveExamenRepository epreuveRepository;
    private final EpreuveSalleRepository epreuveSalleRepository;
    private final InscriptionRepository inscriptionRepository;
    private final ProgrammeEleveService programmeEleveService;

    /**
     * Récupère les élèves concernés par une épreuve.
     */
    @Transactional(readOnly = true)
    public List<Inscription> getElevesEligibles(Long epreuveId) {

        EpreuveExamen epreuve = getEpreuve(epreuveId);

        CoefficientMatiere programme =
                epreuve.getCoefficientMatiere();

        if (programme == null) {
            throw new ExamenBusinessException(
                    "L'épreuve n'a pas de programme associé"
            );
        }

        if (programme.getClasse() == null) {
            throw new ExamenBusinessException(
                    "Le programme de l'épreuve n'est associé à aucune classe"
            );
        }

        if (epreuve.getExamen() == null
                || epreuve.getExamen().getAnneeScolaire() == null) {

            throw new ExamenBusinessException(
                    "L'année scolaire de l'examen est introuvable"
            );
        }

        Long classeId =
                programme.getClasse().getId();

        Long anneeScolaireId =
                epreuve.getExamen()
                        .getAnneeScolaire()
                        .getId();

        List<Inscription> inscriptions =
                inscriptionRepository
                        .findByClasseIdAndAnneeScolaireId(
                                classeId,
                                anneeScolaireId
                        );

        return programmeEleveService.filtrerInscriptions(
                inscriptions,
                programme
        );
    }

    /**
     * Effectue automatiquement la répartition.
     *
     * Les anciennes répartitions de l'épreuve sont remplacées.
     */
    @Transactional
    public List<RepartitionEpreuveResponse> repartirAutomatiquement(
            Long epreuveId
    ) {

        EpreuveExamen epreuve = getEpreuve(epreuveId);

        // ==========================================
        // 1. Récupérer les élèves concernés
        // ==========================================

        List<Inscription> eleves =
                getElevesEligibles(epreuveId);

        if (eleves.isEmpty()) {
            throw new ExamenBusinessException(
                    "Aucun élève n'est concerné par cette épreuve"
            );
        }

        // ==========================================
        // 2. Récupérer les salles de l'épreuve
        // ==========================================

        List<EpreuveSalle> affectations =
                epreuveSalleRepository
                        .findByEpreuveId(epreuveId);

        if (affectations.isEmpty()) {
            throw new ExamenBusinessException(
                    "Aucune salle n'est affectée à cette épreuve"
            );
        }

        List<Salle> salles = affectations.stream()
                .map(EpreuveSalle::getSalle)
                .filter(Salle::isActive)
                .sorted(
                        Comparator.comparing(
                                Salle::getNom,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
                )
                .toList();

        if (salles.isEmpty()) {
            throw new ExamenBusinessException(
                    "Aucune salle active n'est disponible pour cette épreuve"
            );
        }

        // ==========================================
        // 3. Calculer la capacité totale
        // ==========================================

        int capaciteTotale = salles.stream()
                .map(Salle::getCapacite)
                .filter(capacite -> capacite != null && capacite > 0)
                .mapToInt(Integer::intValue)
                .sum();

        if (capaciteTotale < eleves.size()) {

            throw new ExamenBusinessException(
                    "Capacité insuffisante : "
                            + eleves.size()
                            + " élèves pour "
                            + capaciteTotale
                            + " places disponibles"
            );
        }

        // ==========================================
        // 4. Supprimer l'ancienne répartition
        // ==========================================

        repartitionRepository.deleteByEpreuveId(epreuveId);

        // ==========================================
        // 5. Répartition automatique
        // ==========================================

        List<RepartitionEpreuve> repartitions =
                new ArrayList<>();

        int indexEleve = 0;

        for (Salle salle : salles) {

            if (salle.getCapacite() == null
                    || salle.getCapacite() <= 0) {
                continue;
            }

            int nombreAffectes = 0;

            while (
                    indexEleve < eleves.size()
                            && nombreAffectes < salle.getCapacite()
            ) {

                Inscription inscription =
                        eleves.get(indexEleve);

                RepartitionEpreuve repartition =
                        new RepartitionEpreuve();

                repartition.setEpreuve(epreuve);
                repartition.setInscription(inscription);
                repartition.setSalle(salle);

                repartitions.add(repartition);

                indexEleve++;
                nombreAffectes++;
            }
        }

        // Sécurité
        if (indexEleve < eleves.size()) {

            throw new ExamenBusinessException(
                    "Impossible de répartir tous les élèves"
            );
        }

        repartitionRepository.saveAll(repartitions);

        return repartitions.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Récupère la répartition actuelle.
     */
    @Transactional(readOnly = true)
    public List<RepartitionEpreuveResponse> getRepartition(
            Long epreuveId
    ) {

        getEpreuve(epreuveId);

        return repartitionRepository
                .findByEpreuveId(epreuveId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Supprime la répartition d'une épreuve.
     */
    @Transactional
    public void supprimerRepartition(Long epreuveId) {

        getEpreuve(epreuveId);

        repartitionRepository.deleteByEpreuveId(epreuveId);
    }

    private EpreuveExamen getEpreuve(Long id) {

        return epreuveRepository.findById(id)
                .orElseThrow(() ->
                        new ExamenBusinessException(
                                "Épreuve introuvable"
                        )
                );
    }

    private RepartitionEpreuveResponse toResponse(
            RepartitionEpreuve repartition
    ) {

        Inscription inscription =
                repartition.getInscription();

        Salle salle =
                repartition.getSalle();

        return RepartitionEpreuveResponse.builder()

                .id(repartition.getId())

                .epreuveId(
                        repartition.getEpreuve().getId()
                )

                .inscriptionId(
                        inscription.getId()
                )

                .eleveId(
                        inscription.getEleve() != null
                                ? inscription.getEleve().getId()
                                : null
                )

                .eleveNom(
                        inscription.getEleve() != null
                                ? inscription.getEleve().getNom()
                                : null
                )

                .elevePrenom(
                        inscription.getEleve() != null
                                ? inscription.getEleve().getPrenom()
                                : null
                )

                .classeId(
                        inscription.getClasse() != null
                                ? inscription.getClasse().getId()
                                : null
                )

                .classeNom(
                        inscription.getClasse() != null
                                ? inscription.getClasse().getNomComplet()
                                : null
                )

                .salleId(
                        salle.getId()
                )

                .salleNom(
                        salle.getNom()
                )

                .salleCapacite(
                        salle.getCapacite()
                )

                .build();
    }
}