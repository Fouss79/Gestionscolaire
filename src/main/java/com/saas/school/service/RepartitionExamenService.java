package com.saas.school.service;

import com.saas.school.dto.RepartitionExamenDTO;
import com.saas.school.dto.RepartitionExamenRequest;
import com.saas.school.entity.Examen;
import com.saas.school.entity.Inscription;
import com.saas.school.entity.RepartitionExamen;
import com.saas.school.entity.Salle;
import com.saas.school.repository.ExamenRepository;
import com.saas.school.repository.InscriptionRepository;
import com.saas.school.repository.RepartitionExamenRepository;
import com.saas.school.repository.SalleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepartitionExamenService {

    private final ExamenRepository examenRepository;
    private final InscriptionRepository inscriptionRepository;
    private final RepartitionExamenRepository repartitionRepository;
    private final SalleRepository salleRepository;

    /**
     * Capacité utilisée quand une salle n'a pas de capacité renseignée
     * (capacite == null). Ne modifie jamais l'entité Salle elle-même ;
     * sert uniquement au calcul de la répartition.
     */
    private static final int CAPACITE_PAR_DEFAUT = 30;

    private int capaciteEffective(Salle salle) {
        return salle.getCapacite() != null
                ? salle.getCapacite()
                : CAPACITE_PAR_DEFAUT;
    }


    /**
     * Répartition automatique des élèves dans les salles.
     */
    @Transactional
    public List<RepartitionExamenDTO> repartir(
            Long examenId,
            RepartitionExamenRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Les paramètres de répartition sont obligatoires."
            );
        }

        if (request.getSalleIds() == null ||
                request.getSalleIds().isEmpty()) {

            throw new IllegalArgumentException(
                    "Au moins une salle doit être sélectionnée."
            );
        }

        if (request.getClasseIds() == null ||
                request.getClasseIds().isEmpty()) {

            throw new IllegalArgumentException(
                    "Au moins une classe doit être sélectionnée."
            );
        }


        // ---------------------------------------------------------
        // 1. Récupération de l'examen
        // ---------------------------------------------------------

        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Examen introuvable : " + examenId
                        )
                );


        Long ecoleId = examen.getEcole().getId();
        Long anneeId = examen.getAnneeScolaire().getId();


        // ---------------------------------------------------------
        // 2. Récupération des salles
        // ---------------------------------------------------------

        List<Salle> salles = salleRepository.findAllById(
                request.getSalleIds()
        );

        if (salles.size() != request.getSalleIds().size()) {
            throw new IllegalArgumentException(
                    "Une ou plusieurs salles sont introuvables."
            );
        }


        // Vérifier que toutes les salles appartiennent
        // à la même école que l'examen.

        for (Salle salle : salles) {

            if (salle.getEcole() == null ||
                    !Objects.equals(
                            salle.getEcole().getId(),
                            ecoleId
                    )) {

                throw new IllegalArgumentException(
                        "La salle '" + salle.getNom()
                                + "' n'appartient pas à l'école de l'examen."
                );
            }

            /*
             * null = capacité non renseignée -> on utilisera
             * CAPACITE_PAR_DEFAUT plus loin, ce n'est pas une erreur.
             * Une valeur explicite <= 0 reste une vraie erreur de saisie.
             */
            if (salle.getCapacite() != null &&
                    salle.getCapacite() <= 0) {

                throw new IllegalArgumentException(
                        "La salle '" + salle.getNom()
                                + "' possède une capacité invalide."
                );
            }
        }


        // ---------------------------------------------------------
        // 3. Récupérer les élèves des classes sélectionnées
        // ---------------------------------------------------------

        List<Inscription> inscriptions = new ArrayList<>();

        for (Long classeId : request.getClasseIds()) {

            List<Inscription> classeInscriptions =
                    inscriptionRepository
                            .findByClasseIdAndAnneeScolaireId(
                                    classeId,
                                    anneeId
                            );

            inscriptions.addAll(
                    classeInscriptions.stream()
                            .filter(i ->
                                    i.getStatut() ==
                                            StatutInscription.VALIDE
                            )
                            .filter(i ->
                                    i.getEcole() != null &&
                                            Objects.equals(
                                                    i.getEcole().getId(),
                                                    ecoleId
                                            )
                            )
                            .toList()
            );
        }


        // ---------------------------------------------------------
        // 4. Supprimer les doublons
        // ---------------------------------------------------------

        inscriptions = inscriptions.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Inscription::getId,
                        i -> i,
                        (a, b) -> a,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();


        if (inscriptions.isEmpty()) {

            throw new IllegalArgumentException(
                    "Aucun élève valide trouvé dans les classes sélectionnées."
            );
        }


        // ---------------------------------------------------------
        // 5. Vérifier la capacité totale
        // ---------------------------------------------------------

        int capaciteTotale = salles.stream()
                .mapToInt(this::capaciteEffective)
                .sum();

        if (capaciteTotale < inscriptions.size()) {

            throw new IllegalArgumentException(
                    "Capacité insuffisante. "
                            + inscriptions.size()
                            + " élèves à répartir pour seulement "
                            + capaciteTotale
                            + " places disponibles."
            );
        }


        // ---------------------------------------------------------
        // 6. Trier les élèves
        // ---------------------------------------------------------

        String mode = request.getMode();

        if (mode == null || mode.isBlank()) {
            mode = "ALPHABETIQUE";
        }

        mode = mode.toUpperCase(Locale.ROOT);


        if ("ALEATOIRE".equals(mode)) {

            List<Inscription> copie =
                    new ArrayList<>(inscriptions);

            Collections.shuffle(copie);

            inscriptions = copie;

        } else if ("ALPHABETIQUE".equals(mode)) {

            inscriptions = inscriptions.stream()
                    .sorted(
                            Comparator
                                    .comparing(
                                            (Inscription i) ->
                                                    i.getEleve().getNom(),
                                            Comparator.nullsLast(
                                                    String.CASE_INSENSITIVE_ORDER
                                            )
                                    )
                                    .thenComparing(
                                            i -> i.getEleve().getPrenom(),
                                            Comparator.nullsLast(
                                                    String.CASE_INSENSITIVE_ORDER
                                            )
                                    )
                    )
                    .toList();

        } else {

            throw new IllegalArgumentException(
                    "Mode de répartition inconnu : " + mode
                            + ". Utilisez ALPHABETIQUE ou ALEATOIRE."
            );
        }


        // ---------------------------------------------------------
        // 7. Supprimer une ancienne répartition
        // ---------------------------------------------------------

        repartitionRepository.deleteByExamenId(examenId);


        // ---------------------------------------------------------
        // 8. Répartition
        // ---------------------------------------------------------

        List<RepartitionExamen> repartitions =
                new ArrayList<>();

        int indexEleve = 0;


        for (Salle salle : salles) {

            int nombrePlaces =
                    capaciteEffective(salle);

            /*
             * Une salle doit recevoir au moins un élève
             * tant qu'il reste des élèves à répartir.
             */
            int place = 1;

            while (
                    place <= nombrePlaces
                            && indexEleve < inscriptions.size()
            ) {

                Inscription inscription =
                        inscriptions.get(indexEleve);

                RepartitionExamen repartition =
                        new RepartitionExamen();

                repartition.setExamen(examen);
                repartition.setSalle(salle);
                repartition.setInscription(inscription);
                repartition.setNumeroPlace(place);

                repartitions.add(repartition);

                indexEleve++;
                place++;
            }
        }


        // ---------------------------------------------------------
        // 9. Vérification finale
        // ---------------------------------------------------------

        if (indexEleve < inscriptions.size()) {

            throw new IllegalStateException(
                    "Tous les élèves n'ont pas pu être répartis."
            );
        }


        // ---------------------------------------------------------
        // 10. Sauvegarde
        // ---------------------------------------------------------

        List<RepartitionExamen> sauvegardes =
                repartitionRepository.saveAll(repartitions);


        return sauvegardes.stream()
                .map(this::toDTO)
                .toList();
    }


    /**
     * Toutes les affectations d'un examen.
     */
    @Transactional(readOnly = true)
    public List<RepartitionExamenDTO> getRepartition(
            Long examenId
    ) {

        return repartitionRepository
                .findByExamenIdOrderBySalleIdAscNumeroPlaceAsc(
                        examenId
                )
                .stream()
                .map(this::toDTO)
                .toList();
    }


    /**
     * Affectations d'une salle pour un examen.
     */
    @Transactional(readOnly = true)
    public List<RepartitionExamenDTO> getRepartitionSalle(
            Long examenId,
            Long salleId
    ) {

        return repartitionRepository
                .findByExamenIdAndSalleIdOrderByNumeroPlaceAsc(
                        examenId,
                        salleId
                )
                .stream()
                .map(this::toDTO)
                .toList();
    }


    /**
     * Supprimer toute la répartition d'un examen.
     */
    @Transactional
    public void supprimerRepartition(Long examenId) {

        if (!examenRepository.existsById(examenId)) {

            throw new IllegalArgumentException(
                    "Examen introuvable : " + examenId
            );
        }

        repartitionRepository.deleteByExamenId(examenId);
    }


    /**
     * Conversion Entity -> DTO.
     */
    private RepartitionExamenDTO toDTO(
            RepartitionExamen repartition
    ) {

        RepartitionExamenDTO dto =
                new RepartitionExamenDTO();

        dto.setId(repartition.getId());

        dto.setExamenId(
                repartition.getExamen().getId()
        );

        dto.setSalleId(
                repartition.getSalle().getId()
        );

        dto.setSalleNom(
                repartition.getSalle().getNom()
        );

        dto.setNumeroPlace(
                repartition.getNumeroPlace()
        );

        Inscription inscription =
                repartition.getInscription();

        dto.setInscriptionId(
                inscription.getId()
        );

        if (inscription.getEleve() != null) {

            dto.setEleveId(
                    inscription.getEleve().getId()
            );

            dto.setNom(
                    inscription.getEleve().getNom()
            );

            dto.setPrenom(
                    inscription.getEleve().getPrenom()
            );
        }

        if (inscription.getClasse() != null) {

            dto.setClasseId(
                    inscription.getClasse().getId()
            );

            dto.setClasseNom(
                    inscription.getClasse().getNomComplet()
            );
        }

        return dto;
    }
}