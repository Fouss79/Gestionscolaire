package com.saas.school.service;

import com.saas.school.dto.*;
import com.saas.school.entity.*;
import com.saas.school.exception.ExamenBusinessException;
import com.saas.school.repository.*;
import com.saas.school.service.StatutInscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de répartition des élèves dans les salles pour un examen.
 *
 * Option B retenue : seuls les élèves concernés par au moins une EpreuveExamen
 * de l'examen (via le couple niveau/série de leur CoefficientMatiere) sont inclus.
 *
 * NOTE : suppose l'existence de InscriptionRepository avec les deux méthodes
 * additives décrites précédemment (filtrage par niveau/série + statut VALIDE).
 */
@Service
@RequiredArgsConstructor
public class RepartitionService {

    private final ExamenRepository examenRepository;
    private final EpreuveExamenRepository epreuveExamenRepository;
    private final SalleRepository salleRepository;
    private final InscriptionRepository inscriptionRepository;
    private final AffectationEleveExamenRepository affectationRepository;

    // ---------------------------------------------------------------
    // RÉSOLUTION DES ÉLÈVES CONCERNÉS (Option B)
    // ---------------------------------------------------------------

    protected List<Inscription> resolveElevesConcernes(Examen examen) {
        List<Object[]> paires = epreuveExamenRepository.findDistinctNiveauSerieByExamenId(examen.getId());

        if (paires.isEmpty()) {
            throw new ExamenBusinessException("Aucune épreuve définie pour cet examen : impossible de déterminer les élèves concernés");
        }

        // Dédoublonnage : un élève peut être concerné par plusieurs épreuves (plusieurs matières).
        Map<Long, Inscription> parInscriptionId = new LinkedHashMap<>();

        for (Object[] paire : paires) {
            Long niveauId = (Long) paire[0];
            Long serieId = (Long) paire[1]; // peut être null

            List<Inscription> inscriptions = (serieId != null)
                    ? inscriptionRepository.findByAnneeScolaireIdAndStatutAndClasse_Niveau_IdAndClasse_Serie_Id(
                            examen.getAnneeScolaire().getId(), StatutInscription.VALIDE, niveauId, serieId)
                    : inscriptionRepository.findByAnneeScolaireIdAndStatutAndClasse_Niveau_Id(
                            examen.getAnneeScolaire().getId(), StatutInscription.VALIDE, niveauId);

            for (Inscription i : inscriptions) {
                parInscriptionId.putIfAbsent(i.getId(), i);
            }
        }

        return parInscriptionId.values().stream()
                .sorted(Comparator
                        .comparing((Inscription i) -> i.getClasse() != null ? i.getClasse().getNomComplet() : "")
                        .thenComparing(i -> i.getEleve() != null ? i.getEleve().getNom() : "")
                        .thenComparing(i -> i.getEleve() != null ? i.getEleve().getPrenom() : "")
                        .thenComparing(Inscription::getId))
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------
    // RÉPARTITION GLOUTONNE DÉTERMINISTE
    // ---------------------------------------------------------------

    /**
     * Remplit les salles dans l'ordre fourni, jusqu'à leur capacité, sans les mélanger davantage.
     * Retourne, pour chaque salle (dans l'ordre), la liste ordonnée des élèves qui y sont affectés.
     */
    private LinkedHashMap<Salle, List<Inscription>> repartirGlouton(List<Inscription> eleves, List<Salle> salles) {
        LinkedHashMap<Salle, List<Inscription>> resultat = new LinkedHashMap<>();
        for (Salle s : salles) {
            resultat.put(s, new ArrayList<>());
        }

        Iterator<Inscription> it = eleves.iterator();
        for (Salle salle : salles) {
            int capacite = salle.getCapacite() != null ? salle.getCapacite() : 0;
            List<Inscription> bucket = resultat.get(salle);
            while (bucket.size() < capacite && it.hasNext()) {
                bucket.add(it.next());
            }
            if (!it.hasNext()) {
                break;
            }
        }
        return resultat;
    }

    // ---------------------------------------------------------------
    // APERÇU (avant confirmation)
    // ---------------------------------------------------------------

    public RepartitionPreviewResponse preview(Long examenId) {
        Examen examen = getExamen(examenId);
        List<Inscription> eleves = resolveElevesConcernes(examen);
        List<Salle> salles = salleRepository.findByEcoleIdAndActiveTrueOrderByIdAsc(examen.getEcole().getId());

        if (salles.isEmpty()) {
            throw new ExamenBusinessException("Aucune salle active pour cette école");
        }

        int capaciteTotale = salles.stream()
                .mapToInt(s -> s.getCapacite() != null ? s.getCapacite() : 0)
                .sum();

        var repartition = repartirGlouton(eleves, salles);

        List<SalleRepartitionApercu> apercus = repartition.entrySet().stream()
                .map(e -> SalleRepartitionApercu.builder()
                        .salleId(e.getKey().getId())
                        .salleNom(e.getKey().getNom())
                        .capacite(e.getKey().getCapacite())
                        .nombreElevesPrevu(e.getValue().size())
                        .build())
                .toList();

        return RepartitionPreviewResponse.builder()
                .examenId(examenId)
                .nombreEleves(eleves.size())
                .capaciteTotale(capaciteTotale)
                .nombreSalles(salles.size())
                .capaciteSuffisante(capaciteTotale >= eleves.size())
                .repartitionPrevue(apercus)
                .build();
    }

    // ---------------------------------------------------------------
    // GÉNÉRATION (transactionnelle, remplace toute répartition existante)
    // ---------------------------------------------------------------

    @Transactional
    public RepartitionResultResponse genererRepartition(Long examenId) {
        Examen examen = getExamen(examenId);
        List<Inscription> eleves = resolveElevesConcernes(examen);
        List<Salle> salles = salleRepository.findByEcoleIdAndActiveTrueOrderByIdAsc(examen.getEcole().getId());

        if (salles.isEmpty()) {
            throw new ExamenBusinessException("Aucune salle active pour cette école");
        }

        int capaciteTotale = salles.stream()
                .mapToInt(s -> s.getCapacite() != null ? s.getCapacite() : 0)
                .sum();

        if (capaciteTotale < eleves.size()) {
            throw new ExamenBusinessException(
                    "Capacité totale insuffisante : %d élèves pour %d places".formatted(eleves.size(), capaciteTotale));
        }

        var repartition = repartirGlouton(eleves, salles);

        // Aucune affectation partielle : on supprime l'existant avant de réinsérer,
        // le tout dans la même transaction.
        affectationRepository.deleteByExamenId(examenId);

        List<AffectationEleveExamen> aCreer = new ArrayList<>();
        List<SalleRepartitionApercu> apercus = new ArrayList<>();

        for (var entry : repartition.entrySet()) {
            Salle salle = entry.getKey();
            List<Inscription> elevesSalle = entry.getValue();

            int place = 1;
            for (Inscription inscription : elevesSalle) {
                AffectationEleveExamen affectation = new AffectationEleveExamen();
                affectation.setExamen(examen);
                affectation.setInscription(inscription);
                affectation.setSalle(salle);
                affectation.setNumeroPlace(place++);
                aCreer.add(affectation);
            }

            apercus.add(SalleRepartitionApercu.builder()
                    .salleId(salle.getId())
                    .salleNom(salle.getNom())
                    .capacite(salle.getCapacite())
                    .nombreElevesPrevu(elevesSalle.size())
                    .build());
        }

        affectationRepository.saveAll(aCreer);

        return RepartitionResultResponse.builder()
                .examenId(examenId)
                .nombreElevesAffectes(aCreer.size())
                .repartitionParSalle(apercus)
                .build();
    }

    // ---------------------------------------------------------------
    // CONSULTATION
    // ---------------------------------------------------------------

    public List<AffectationEleveExamenResponse> getAffectations(Long examenId) {
        return affectationRepository.findByExamenId(examenId).stream()
                .map(AffectationEleveExamenResponse::from)
                .toList();
    }

    public List<AffectationEleveExamenResponse> getAffectationsBySalle(Long examenId, Long salleId) {
        return affectationRepository.findByExamenIdAndSalleId(examenId, salleId).stream()
                .map(AffectationEleveExamenResponse::from)
                .toList();
    }

    public AffectationEleveExamenResponse getAffectationByEleve(Long examenId, Long inscriptionId) {
        return affectationRepository.findByExamenIdAndInscriptionId(examenId, inscriptionId)
                .map(AffectationEleveExamenResponse::from)
                .orElseThrow(() -> new ExamenBusinessException("Aucune affectation trouvée pour cet élève sur cet examen"));
    }

    private Examen getExamen(Long id) {
        return examenRepository.findById(id)
                .orElseThrow(() -> new ExamenBusinessException("Examen introuvable"));
    }
}
