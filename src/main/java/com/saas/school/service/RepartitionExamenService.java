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
 * Répartition des élèves d'un examen dans le pool de salles sélectionné.
 *
 * Architecture :
 *
 * Examen
 *   ├── classes sélectionnées
 *   ├── salles sélectionnées via ExamenSalle
 *   └── RepartitionExamen
 *          ├── élève
 *          └── salle
 *
 * Les salles constituent un pool commun.
 *
 * L'algorithme cherche à :
 * - utiliser plusieurs salles lorsque nécessaire ;
 * - ne pas attendre qu'une salle soit pleine avant d'utiliser une autre ;
 * - mélanger les élèves provenant de plusieurs classes ;
 * - respecter la capacité maximale de chaque salle.
 *
 * Classe.salle_id n'est PAS utilisé pour la répartition de l'examen.
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
    // ================================================================

    @Transactional(readOnly = true)
    public List<Inscription> getElevesEligibles(Long examenId) {

        Examen examen = getExamen(examenId);

        if (examen.getAnneeScolaire() == null) {
            throw new ExamenBusinessException(
                    "L'année scolaire de l'examen est introuvable"
            );
        }

        Set<Classe> classes = examen.getClasses();

        if (classes == null || classes.isEmpty()) {
            throw new ExamenBusinessException(
                    "Aucune classe n'est associée à cet examen"
            );
        }

        return inscriptionsParClasseTriees(
                classes,
                examen.getAnneeScolaire().getId()
        );
    }

    // ================================================================
    // RÉPARTITION AUTOMATIQUE
    // ================================================================

    @Transactional
    public List<RepartitionExamenResponse> repartirAutomatiquement(
            Long examenId
    ) {

        Examen examen = getExamen(examenId);

        if (examen.getAnneeScolaire() == null) {
            throw new ExamenBusinessException(
                    "L'année scolaire de l'examen est introuvable"
            );
        }

        Set<Classe> classes = examen.getClasses();

        if (classes == null || classes.isEmpty()) {
            throw new ExamenBusinessException(
                    "Aucune classe n'est associée à cet examen"
            );
        }

        // ------------------------------------------------------------
        // Salles sélectionnées pour l'examen
        // ------------------------------------------------------------

        List<ExamenSalle> affectationsSalles =
                examenSalleRepository.findByExamenId(examenId);

        if (affectationsSalles.isEmpty()) {
            throw new ExamenBusinessException(
                    "Aucune salle n'est sélectionnée pour cet examen"
            );
        }

        List<Salle> salles = affectationsSalles.stream()
                .map(ExamenSalle::getSalle)
                .filter(Objects::nonNull)
                .filter(Salle::isActive)
                .filter(salle ->
                        salle.getCapacite() != null
                                && salle.getCapacite() > 0
                )
                .distinct()
                .sorted(
                        Comparator.comparing(
                                Salle::getNom,
                                Comparator.nullsLast(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                )
                .toList();

        if (salles.isEmpty()) {
            throw new ExamenBusinessException(
                    "Aucune salle active avec une capacité valide " +
                            "n'est disponible pour cet examen"
            );
        }

        // ------------------------------------------------------------
        // Élèves concernés
        // ------------------------------------------------------------

        Map<Long, List<Inscription>> elevesParClasse =
                recupererElevesParClasse(
                        classes,
                        examen.getAnneeScolaire().getId()
                );

        if (elevesParClasse.isEmpty()) {
            throw new ExamenBusinessException(
                    "Aucun élève n'est concerné par cet examen"
            );
        }

        int nombreTotalEleves = elevesParClasse.values()
                .stream()
                .mapToInt(List::size)
                .sum();

        // ------------------------------------------------------------
        // Capacité totale
        // ------------------------------------------------------------

        int capaciteTotale = salles.stream()
                .map(Salle::getCapacite)
                .filter(Objects::nonNull)
                .filter(c -> c > 0)
                .mapToInt(Integer::intValue)
                .sum();

        if (capaciteTotale < nombreTotalEleves) {
            throw new ExamenBusinessException(
                    "Capacité insuffisante : "
                            + nombreTotalEleves
                            + " élèves pour "
                            + capaciteTotale
                            + " places disponibles"
            );
        }

        // ------------------------------------------------------------
        // Nouvelle répartition
        // ------------------------------------------------------------

        List<RepartitionExamen> nouvellesRepartitions =
                repartirElevesEquitablement(
                        examen,
                        elevesParClasse,
                        salles
                );

        if (nouvellesRepartitions.size() != nombreTotalEleves) {
            throw new ExamenBusinessException(
                    "Impossible de répartir tous les élèves"
            );
        }

        // ------------------------------------------------------------
        // Remplacer l'ancienne répartition
        // ------------------------------------------------------------

        repartitionRepository.deleteByExamenId(examenId);

        repartitionRepository.saveAll(nouvellesRepartitions);

        return nouvellesRepartitions.stream()
                .map(RepartitionExamenResponse::from)
                .toList();
    }

    // ================================================================
    // ALGORITHME DE RÉPARTITION
    // ================================================================

    /**
     * Répartit les élèves sur toutes les salles disponibles.
     *
     * Principe :
     *
     * 1. Les élèves sont regroupés par classe.
     * 2. Les salles sont utilisées comme un pool commun.
     * 3. On effectue plusieurs passages sur les classes.
     * 4. À chaque passage, on donne un élève de chaque classe
     *    à la salle suivante disponible.
     * 5. On passe régulièrement d'une salle à l'autre.
     *
     * Ainsi, une salle n'a pas besoin d'être pleine avant que
     * les autres salles commencent à recevoir des élèves.
     */
    private List<RepartitionExamen> repartirElevesEquitablement(
            Examen examen,
            Map<Long, List<Inscription>> elevesParClasse,
            List<Salle> salles
    ) {

        // ------------------------------------------------------------
        // Files d'élèves par classe
        // ------------------------------------------------------------

        List<Queue<Inscription>> files = elevesParClasse.values()
                .stream()
                .map(LinkedList::new)
                .collect(Collectors.toCollection(ArrayList::new));

        List<RepartitionExamen> repartitions = new ArrayList<>();

        // ------------------------------------------------------------
        // Places restantes par salle
        // ------------------------------------------------------------

        int[] placesRestantes = new int[salles.size()];

        for (int i = 0; i < salles.size(); i++) {
            placesRestantes[i] = salles.get(i).getCapacite();
        }

        int indexSalle = 0;

        // ------------------------------------------------------------
        // Tant qu'il reste des élèves
        // ------------------------------------------------------------

        while (!toutesLesFilesVides(files)) {

            boolean affectationEffectuee = false;

            /*
             * On fait un tour des classes.
             *
             * Pour chaque classe, on cherche une salle disponible.
             */
            for (Queue<Inscription> file : files) {

                if (file.isEmpty()) {
                    continue;
                }

                int salleTrouvee = trouverProchaineSalleDisponible(
                        placesRestantes,
                        indexSalle
                );

                if (salleTrouvee == -1) {
                    throw new ExamenBusinessException(
                            "Impossible de trouver une salle disponible"
                    );
                }

                Inscription inscription = file.poll();

                RepartitionExamen repartition =
                        new RepartitionExamen();

                repartition.setExamen(examen);
                repartition.setInscription(inscription);
                repartition.setSalle(salles.get(salleTrouvee));

                repartitions.add(repartition);

                placesRestantes[salleTrouvee]--;

                /*
                 * La prochaine classe commencera à chercher
                 * à partir de la salle suivante.
                 */
                indexSalle = (salleTrouvee + 1) % salles.size();

                affectationEffectuee = true;
            }

            if (!affectationEffectuee) {
                break;
            }
        }

        return repartitions;
    }

    /**
     * Cherche une salle ayant encore de la place.
     *
     * On commence à partir de startIndex et on tourne
     * dans le pool des salles.
     */
    private int trouverProchaineSalleDisponible(
            int[] placesRestantes,
            int startIndex
    ) {

        for (int i = 0; i < placesRestantes.length; i++) {

            int index =
                    (startIndex + i) % placesRestantes.length;

            if (placesRestantes[index] > 0) {
                return index;
            }
        }

        return -1;
    }

    private boolean toutesLesFilesVides(
            List<Queue<Inscription>> files
    ) {

        return files.stream()
                .allMatch(Queue::isEmpty);
    }

    // ================================================================
    // CONSULTATION
    // ================================================================

    @Transactional(readOnly = true)
    public List<RepartitionExamenResponse> getRepartition(
            Long examenId
    ) {

        getExamen(examenId);

        return repartitionRepository.findByExamenId(examenId)
                .stream()
                .map(RepartitionExamenResponse::from)
                .toList();
    }

    // ================================================================
    // RÉPARTITION PAR ÉPREUVE
    // ================================================================

    /**
     * Retourne la répartition des élèves concernés par une épreuve.
     *
     * La salle reste celle attribuée au niveau de l'examen.
     * On ne crée PAS une nouvelle répartition par épreuve.
     */
    @Transactional(readOnly = true)
    public List<RepartitionExamenResponse> getRepartitionParEpreuve(
            Long examenId,
            Long epreuveId
    ) {

        Examen examen = getExamen(examenId);

        EpreuveExamen epreuve =
                epreuveExamenRepository.findById(epreuveId)
                        .orElseThrow(() ->
                                new ExamenBusinessException(
                                        "Épreuve introuvable"
                                )
                        );

        if (epreuve.getExamen() == null
                || !epreuve.getExamen().getId().equals(examenId)) {

            throw new ExamenBusinessException(
                    "Cette épreuve n'appartient pas à cet examen"
            );
        }

        CoefficientMatiere programme =
                epreuve.getCoefficientMatiere();

        if (programme == null) {
            throw new ExamenBusinessException(
                    "L'épreuve n'a pas de programme associé"
            );
        }

        Set<Long> classeIdsConcernees =
                examen.getClasses() == null
                        ? Set.of()
                        : examen.getClasses()
                        .stream()
                        .filter(c ->
                                estCompatibleNiveauSerie(
                                        c,
                                        programme
                                )
                        )
                        .map(Classe::getId)
                        .collect(Collectors.toSet());

        if (classeIdsConcernees.isEmpty()) {
            return List.of();
        }

        return repartitionRepository
                .findByExamenId(examenId)
                .stream()
                .filter(r ->
                        r.getInscription() != null
                                && r.getInscription().getClasse() != null
                                && classeIdsConcernees.contains(
                                r.getInscription()
                                        .getClasse()
                                        .getId()
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

    // ================================================================
    // RÉPARTITION GROUPÉE PAR SALLE
    // ================================================================

    /**
     * Retourne une liste de groupes :
     *
     * Salle 1
     *   ├── élève
     *   ├── élève
     *   └── ...
     *
     * Salle 2
     *   ├── élève
     *   └── ...
     */
    @Transactional(readOnly = true)
    public List<RepartitionSalleGroupResponse>
    getRepartitionGroupeeParSalle(Long examenId) {

        getExamen(examenId);

        List<RepartitionExamen> repartitions =
                repartitionRepository.findByExamenId(examenId);

        if (repartitions.isEmpty()) {
            return List.of();
        }

        Map<Long, List<RepartitionExamen>> parSalle =
                repartitions.stream()
                        .filter(r -> r.getSalle() != null)
                        .collect(
                                Collectors.groupingBy(
                                        r -> r.getSalle().getId(),
                                        LinkedHashMap::new,
                                        Collectors.toList()
                                )
                        );

        return parSalle.values()
                .stream()
                .map(groupe -> {

                    Salle salle = groupe.get(0).getSalle();

                    List<RepartitionExamenResponse> eleves =
                            groupe.stream()
                                    .map(RepartitionExamenResponse::from)
                                    .sorted(
                                            Comparator.comparing(
                                                    r -> {
                                                        String nom =
                                                                r.getEleveNom() == null
                                                                        ? ""
                                                                        : r.getEleveNom();

                                                        String prenom =
                                                                r.getElevePrenom() == null
                                                                        ? ""
                                                                        : r.getElevePrenom();

                                                        return (
                                                                nom
                                                                        + " "
                                                                        + prenom
                                                        ).toLowerCase(
                                                                Locale.ROOT
                                                        );
                                                    }
                                            )
                                    )
                                    .toList();

                    return RepartitionSalleGroupResponse
                            .builder()
                            .salleId(salle.getId())
                            .salleNom(salle.getNom())
                            .salleCapacite(salle.getCapacite())
                            .nombreEleves(eleves.size())
                            .eleves(eleves)
                            .build();
                })
                .sorted(
                        Comparator.comparing(
                                RepartitionSalleGroupResponse::getSalleNom,
                                Comparator.nullsLast(
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                )
                .toList();
    }

    // ================================================================
    // SUPPRESSION
    // ================================================================

    @Transactional
    public void supprimerRepartition(Long examenId) {

        getExamen(examenId);

        repartitionRepository.deleteByExamenId(examenId);
    }

    // ================================================================
    // RÉCUPÉRATION DES ÉLÈVES PAR CLASSE
    // ================================================================

    private Map<Long, List<Inscription>> recupererElevesParClasse(
            Set<Classe> classes,
            Long anneeScolaireId
    ) {

        List<Classe> classesTriees =
                classes.stream()
                        .sorted(
                                Comparator.comparing(
                                        Classe::getNomComplet,
                                        Comparator.nullsLast(
                                                String.CASE_INSENSITIVE_ORDER
                                        )
                                )
                        )
                        .toList();

        Map<Long, List<Inscription>> resultat =
                new LinkedHashMap<>();

        for (Classe classe : classesTriees) {

            List<Inscription> inscriptions =
                    inscriptionRepository
                            .findByClasseIdAndAnneeScolaireId(
                                    classe.getId(),
                                    anneeScolaireId
                            );

            if (!inscriptions.isEmpty()) {
                resultat.put(
                        classe.getId(),
                        new ArrayList<>(inscriptions)
                );
            }
        }

        return resultat;
    }

    // ================================================================
    // UTILITAIRE — ÉLÈVES DE TOUTES LES CLASSES
    // ================================================================

    private List<Inscription> inscriptionsParClasseTriees(
            Set<Classe> classes,
            Long anneeScolaireId
    ) {

        List<Classe> classesTriees =
                classes.stream()
                        .sorted(
                                Comparator.comparing(
                                        Classe::getNomComplet,
                                        Comparator.nullsLast(
                                                String.CASE_INSENSITIVE_ORDER
                                        )
                                )
                        )
                        .toList();

        List<Inscription> toutes =
                new ArrayList<>();

        for (Classe classe : classesTriees) {

            toutes.addAll(
                    inscriptionRepository
                            .findByClasseIdAndAnneeScolaireId(
                                    classe.getId(),
                                    anneeScolaireId
                            )
            );
        }

        return toutes;
    }

    // ================================================================
    // COMPATIBILITÉ PROGRAMME / CLASSE
    // ================================================================

    private boolean estCompatibleNiveauSerie(
            Classe classe,
            CoefficientMatiere programme
    ) {

        if (classe.getNiveau() == null
                || programme.getNiveau() == null) {

            return false;
        }

        if (!classe.getNiveau()
                .getId()
                .equals(programme.getNiveau().getId())) {

            return false;
        }

        if (programme.getSerie() == null) {
            return true;
        }

        return classe.getSerie() != null
                && classe.getSerie()
                .getId()
                .equals(programme.getSerie().getId());
    }

    // ================================================================
    // EXAMEN
    // ================================================================

    private Examen getExamen(Long id) {

        return examenRepository.findById(id)
                .orElseThrow(() ->
                        new ExamenBusinessException(
                                "Examen introuvable"
                        )
                );
    }
}