package com.saas.school.service;

import com.saas.school.dto.RepartitionExamenResponse;
import com.saas.school.dto.RepartitionSalleGroupResponse;
import com.saas.school.entity.*;
import com.saas.school.exception.ExamenBusinessException;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Répartition des élèves de l'examen dans le pool de salles sélectionné.
 *
 * Architecture (voir schéma validé) :
 *   Examen
 *     ├── classes sélectionnées (Examen.classes)
 *     ├── salles sélectionnées (Examen.salles)
 *     └── répartition élève → salle (RepartitionExamen), calculée une
 *         seule fois pour tout l'examen, consultable filtrée par épreuve.
 *
 * Les salles ne sont PAS réservées à une classe précise : c'est un pool
 * commun. L'algorithme regroupe les élèves classe par classe (pour
 * limiter le mélange) puis remplit les salles dans l'ordre, mais une
 * salle peut contenir des élèves de deux classes différentes si les
 * effectifs ne tombent pas juste sur les capacités.
 */
@Service
@RequiredArgsConstructor
public class RepartitionExamenService {

    private final RepartitionExamenRepository repartitionRepository;
    private final ExamenRepository examenRepository;
    private final EpreuveExamenRepository epreuveExamenRepository;
    private final ExamenSalleRepository examenSalleRepository;
    private final InscriptionRepository inscriptionRepository;
    private final ProgrammeEleveService programmeEleveService;

    // ================================================================
    // ÉLÈVES ÉLIGIBLES POUR TOUT L'EXAMEN
    // (tous les élèves de toutes les classes de l'examen)
    // ================================================================


    @Transactional(readOnly = true)
    public List<Inscription> getElevesEligibles(Long examenId) {

        Examen examen = getExamen(examenId);

        System.out.println("=== REPARTITION EXAMEN ===");
        System.out.println("Examen ID = " + examen.getId());
        System.out.println("Année ID = " +
                (examen.getAnneeScolaire() != null
                        ? examen.getAnneeScolaire().getId()
                        : null));

        if (examen.getAnneeScolaire() == null) {
            throw new ExamenBusinessException(
                    "L'année scolaire de l'examen est introuvable"
            );
        }

        Set<Classe> classes = examen.getClasses();

        System.out.println("Nombre de classes = " +
                (classes != null ? classes.size() : 0));

        if (classes == null || classes.isEmpty()) {
            throw new ExamenBusinessException(
                    "Aucune classe n'est associée à cet examen"
            );
        }

        for (Classe classe : classes) {

            System.out.println(
                    "Classe ID = " + classe.getId()
            );

            List<Inscription> inscriptions =
                    inscriptionRepository.findByClasseIdAndAnneeScolaireId(
                            classe.getId(),
                            examen.getAnneeScolaire().getId()
                    );

            System.out.println(
                    "Inscriptions trouvées = " + inscriptions.size()
            );

            for (Inscription inscription : inscriptions) {
                System.out.println(
                        "  inscription=" + inscription.getId()
                                + " statut=" + inscription.getStatut()
                );
            }
        }

        List<Inscription> result =
                inscriptionsParClasseTriees(
                        classes,
                        examen.getAnneeScolaire().getId()
                );

        System.out.println(
                "TOTAL ÉLÈVES = " + result.size()
        );

        return result;
    }
    // ================================================================
    // RÉPARTITION AUTOMATIQUE
    // ================================================================

    @Transactional
    public List<RepartitionExamenResponse> repartirAutomatiquement(Long examenId) {

        Examen examen = getExamen(examenId);

        Set<Classe> classes = examen.getClasses();

        if (classes == null || classes.isEmpty()) {
            throw new ExamenBusinessException(
                    "Aucune classe n'est associée à cet examen"
            );
        }

        if (examen.getAnneeScolaire() == null) {
            throw new ExamenBusinessException(
                    "L'année scolaire de l'examen est introuvable"
            );
        }

        List<ExamenSalle> affectationsSalles =
                examenSalleRepository.findByExamenId(examenId);

        if (affectationsSalles.isEmpty()) {
            throw new ExamenBusinessException(
                    "Aucune salle n'est sélectionnée pour cet examen"
            );
        }

        List<Salle> salles = affectationsSalles.stream()
                .map(ExamenSalle::getSalle)
                .filter(Salle::isActive)
                .sorted(Comparator.comparing(
                        Salle::getNom,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .toList();

        if (salles.isEmpty()) {
            throw new ExamenBusinessException(
                    "Aucune salle active n'est disponible pour cet examen"
            );
        }

        // Élèves groupés classe par classe (ordre stable) pour limiter
        // le mélange entre classes dans une même salle.
        List<Inscription> eleves =
                inscriptionsParClasseTriees(classes, examen.getAnneeScolaire().getId());

        if (eleves.isEmpty()) {
            throw new ExamenBusinessException(
                    "Aucun élève n'est concerné par cet examen"
            );
        }

        int capaciteTotale = salles.stream()
                .map(Salle::getCapacite)
                .filter(c -> c != null && c > 0)
                .mapToInt(Integer::intValue)
                .sum();

        if (capaciteTotale < eleves.size()) {
            throw new ExamenBusinessException(
                    "Capacité insuffisante : " + eleves.size()
                            + " élèves pour " + capaciteTotale
                            + " places disponibles"
            );
        }

        List<RepartitionExamen> nouvellesRepartitions = new ArrayList<>();

        int indexEleve = 0;

        for (Salle salle : salles) {

            if (salle.getCapacite() == null || salle.getCapacite() <= 0) {
                continue;
            }

            int nombreAffectes = 0;

            while (indexEleve < eleves.size()
                    && nombreAffectes < salle.getCapacite()) {

                Inscription inscription = eleves.get(indexEleve);

                RepartitionExamen repartition = new RepartitionExamen();
                repartition.setExamen(examen);
                repartition.setInscription(inscription);
                repartition.setSalle(salle);

                nouvellesRepartitions.add(repartition);

                indexEleve++;
                nombreAffectes++;
            }
        }

        if (indexEleve < eleves.size()) {
            throw new ExamenBusinessException(
                    "Impossible de répartir tous les élèves"
            );
        }

        repartitionRepository.deleteByExamenId(examenId);
        repartitionRepository.saveAll(nouvellesRepartitions);

        return nouvellesRepartitions.stream()
                .map(RepartitionExamenResponse::from)
                .toList();
    }

    // ================================================================
    // CONSULTATION
    // ================================================================

    @Transactional(readOnly = true)
    public List<RepartitionExamenResponse> getRepartition(Long examenId) {

        getExamen(examenId);

        return repartitionRepository.findByExamenId(examenId)
                .stream()
                .map(RepartitionExamenResponse::from)
                .toList();
    }

    /**
     * Répartition filtrée pour une épreuve donnée : ne garde que les
     * classes de l'examen dont le niveau + série correspondent au
     * programme de l'épreuve, puis applique le filtrage fin
     * (sous-groupe, options...) via ProgrammeEleveService.
     *
     * Utile pour générer une feuille de présence par épreuve.
     */
    @Transactional(readOnly = true)
    public List<RepartitionExamenResponse> getRepartitionParEpreuve(
            Long examenId,
            Long epreuveId
    ) {

        Examen examen = getExamen(examenId);

        EpreuveExamen epreuve = epreuveExamenRepository.findById(epreuveId)
                .orElseThrow(() ->
                        new ExamenBusinessException("Épreuve introuvable"));

        if (epreuve.getExamen() == null
                || !epreuve.getExamen().getId().equals(examenId)) {

            throw new ExamenBusinessException(
                    "Cette épreuve n'appartient pas à cet examen"
            );
        }

        CoefficientMatiere programme = epreuve.getCoefficientMatiere();

        if (programme == null) {
            throw new ExamenBusinessException(
                    "L'épreuve n'a pas de programme associé"
            );
        }

        Set<Long> classeIdsConcernees =
                examen.getClasses() == null
                        ? Set.of()
                        : examen.getClasses().stream()
                        .filter(c -> estCompatibleNiveauSerie(c, programme))
                        .map(Classe::getId)
                        .collect(Collectors.toSet());

        if (classeIdsConcernees.isEmpty()) {
            return List.of();
        }

        return repartitionRepository.findByExamenId(examenId).stream()
                .filter(r ->
                        r.getInscription() != null
                                && r.getInscription().getClasse() != null
                                && classeIdsConcernees.contains(
                                r.getInscription().getClasse().getId()
                        )
                )
                .filter(r ->
                        programmeEleveService.estCompatible(
                                r.getInscription(),
                                programme
                        )
                )
                .map(RepartitionExamenResponse::from)
                .toList();
    }

    /**
     * Répartition groupée par salle : une entrée par salle utilisée,
     * avec la liste des élèves qui s'y trouvent. Pratique pour l'affichage
     * "Salle 101 : 25 élèves" et l'impression des feuilles de salle.
     */
    @Transactional(readOnly = true)
    public List<RepartitionSalleGroupResponse> getRepartitionGroupeeParSalle(
            Long examenId
    ) {

        getExamen(examenId);

        List<RepartitionExamen> repartitions =
                repartitionRepository.findByExamenId(examenId);

        Map<Long, List<RepartitionExamen>> parSalle = repartitions.stream()
                .collect(Collectors.groupingBy(r -> r.getSalle().getId()));

        return parSalle.values().stream()
                .map(groupe -> {

                    Salle salle = groupe.get(0).getSalle();

                    List<RepartitionExamenResponse> eleves = groupe.stream()
                            .map(RepartitionExamenResponse::from)
                            .sorted(Comparator.comparing(
                                    r -> (r.getEleveNom() == null ? "" : r.getEleveNom())
                                            + (r.getElevePrenom() == null ? "" : r.getElevePrenom())
                            ))
                            .toList();

                    return RepartitionSalleGroupResponse.builder()
                            .salleId(salle.getId())
                            .salleNom(salle.getNom())
                            .salleCapacite(salle.getCapacite())
                            .nombreEleves(eleves.size())
                            .eleves(eleves)
                            .build();
                })
                .sorted(Comparator.comparing(
                        RepartitionSalleGroupResponse::getSalleNom,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .toList();
    }

    @Transactional
    public void supprimerRepartition(Long examenId) {

        getExamen(examenId);

        repartitionRepository.deleteByExamenId(examenId);
    }

    // ================================================================
    // UTILITAIRES
    // ================================================================

    /**
     * Récupère les inscriptions de toutes les classes données, groupées
     * classe par classe (dans l'ordre des classes fourni), afin de
     * limiter le mélange entre classes lors du remplissage des salles.
     */
    private List<Inscription> inscriptionsParClasseTriees(
            Set<Classe> classes,
            Long anneeScolaireId
    ) {

        List<Classe> classesTriees = classes.stream()
                .sorted(Comparator.comparing(
                        Classe::getNomComplet,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .toList();

        List<Inscription> toutes = new ArrayList<>();

        for (Classe classe : classesTriees) {
            toutes.addAll(
                    inscriptionRepository.findByClasseIdAndAnneeScolaireId(
                            classe.getId(),
                            anneeScolaireId
                    )
            );
        }

        return toutes;
    }

    private boolean estCompatibleNiveauSerie(
            Classe classe,
            CoefficientMatiere programme
    ) {

        if (classe.getNiveau() == null || programme.getNiveau() == null) {
            return false;
        }

        if (!classe.getNiveau().getId().equals(programme.getNiveau().getId())) {
            return false;
        }

        if (programme.getSerie() == null) {
            return true;
        }

        return classe.getSerie() != null
                && classe.getSerie().getId().equals(programme.getSerie().getId());
    }

    private Examen getExamen(Long id) {

        return examenRepository.findById(id)
                .orElseThrow(() ->
                        new ExamenBusinessException("Examen introuvable"));
    }
}