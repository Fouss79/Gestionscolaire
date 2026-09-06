package com.saas.school.service;

import com.saas.school.dto.EmargementResumeDTO;
import com.saas.school.dto.PaiementEnseignantDTO;
import com.saas.school.entity.Emargement;
import com.saas.school.entity.Enseignant;
import com.saas.school.entity.PaiementEnseignant;
import com.saas.school.repository.EmargementRepository;
import com.saas.school.repository.EnseignantRepository;
import com.saas.school.repository.PaiementEnseignantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaiementEnseignantService {

    private final PaiementEnseignantRepository paiementRepo;
    private final EnseignantRepository enseignantRepo;
    private final EmargementRepository emargementRepo;

    private final EmargementService emargementService;
    private final OperationComptableService operationComptableService;


    // ============================================================
    // PRÉVISUALISATION
    // ============================================================

    public List<PaiementEnseignantDTO> previsualiserTous(
            LocalDate debut,
            LocalDate fin,
            Long anneeId) {

        List<EmargementResumeDTO> resumes =
                emargementService.getResumeTousEnseignants(
                        debut,
                        fin,
                        anneeId
                );

        List<PaiementEnseignantDTO> resultats = new ArrayList<>();

        for (EmargementResumeDTO r : resumes) {

            Enseignant ens = enseignantRepo.findById(r.getEnseignantId())
                    .orElseThrow(() ->
                            new RuntimeException("Enseignant introuvable"));

            // ----------------------------------------------------
            // ÉMARGEMENTS DE LA PÉRIODE
            // ----------------------------------------------------

            List<Emargement> emargements =
                    emargementRepo
                            .findByEmploiDuTemps_Enseignant_IdAndDateHeureBetweenAndEmploiDuTemps_AnneeScolaireId(
                                    r.getEnseignantId(),
                                    debut,
                                    fin,
                                    anneeId
                            );

            // ----------------------------------------------------
            // ÉMARGEMENTS DÉJÀ PAYÉS / GÉNÉRÉS
            // ----------------------------------------------------

            Set<Long> emargementsDejaUtilises =
                    getEmargementIdsDejaUtilises(
                            r.getEnseignantId(),
                            anneeId
                    );

            // ----------------------------------------------------
            // GARDER UNIQUEMENT LES NOUVEAUX
            // ----------------------------------------------------

            List<Emargement> nouveauxEmargements =
                    emargements.stream()
                            .filter(em ->
                                    em.getId() != null &&
                                            !emargementsDejaUtilises.contains(
                                                    em.getId()
                                            )
                            )
                            .toList();

            // ----------------------------------------------------
            // CALCUL DES HEURES
            // ----------------------------------------------------

            int totalHeures = nouveauxEmargements.stream()
                    .mapToInt(Emargement::getDuree)
                    .sum();

            double taux =
                    ens.getTauxHoraire() != null
                            ? ens.getTauxHoraire()
                            : 0.0;

            double montant = totalHeures * taux;

            // ----------------------------------------------------
            // DTO
            // ----------------------------------------------------

            resultats.add(
                    PaiementEnseignantDTO.builder()
                            .enseignantId(r.getEnseignantId())
                            .enseignantNom(r.getEnseignantNom())
                            .enseignantPrenom(r.getEnseignantPrenom())
                            .periodeDebut(debut)
                            .periodeFin(fin)
                            .totalHeures(totalHeures)
                            .tauxHoraire(taux)
                            .montant(montant)
                            .statut("NON_GENERE")
                            .build()
            );
        }

        return resultats;
    }


    // ============================================================
    // GÉNÉRATION
    // ============================================================

    @Transactional
    public List<PaiementEnseignantDTO> genererPaiements(
            LocalDate debut,
            LocalDate fin,
            Long anneeId) {

        List<EmargementResumeDTO> resumes =
                emargementService.getResumeTousEnseignants(
                        debut,
                        fin,
                        anneeId
                );

        List<PaiementEnseignantDTO> resultats =
                new ArrayList<>();


        for (EmargementResumeDTO r : resumes) {

            Enseignant ens = enseignantRepo.findById(
                    r.getEnseignantId()
            ).orElseThrow(() ->
                    new RuntimeException("Enseignant introuvable")
            );


            // ----------------------------------------------------
            // ÉMARGEMENTS DE LA PÉRIODE
            // ----------------------------------------------------

            List<Emargement> emargements =
                    emargementRepo
                            .findByEmploiDuTemps_Enseignant_IdAndDateHeureBetweenAndEmploiDuTemps_AnneeScolaireId(
                                    r.getEnseignantId(),
                                    debut,
                                    fin,
                                    anneeId
                            );


            // ----------------------------------------------------
            // ÉMARGEMENTS DÉJÀ UTILISÉS
            // ----------------------------------------------------

            Set<Long> emargementsDejaUtilises =
                    getEmargementIdsDejaUtilises(
                            r.getEnseignantId(),
                            anneeId
                    );


            // ----------------------------------------------------
            // NOUVEAUX ÉMARGEMENTS
            // ----------------------------------------------------

            List<Emargement> nouveauxEmargements =
                    emargements.stream()
                            .filter(em ->
                                    em.getId() != null &&
                                            !emargementsDejaUtilises.contains(
                                                    em.getId()
                                            )
                            )
                            .toList();


            // ----------------------------------------------------
            // AUCUNE NOUVELLE HEURE
            // ----------------------------------------------------

            if (nouveauxEmargements.isEmpty()) {
                continue;
            }


            // ----------------------------------------------------
            // CALCUL HEURES
            // ----------------------------------------------------

            int totalHeures =
                    nouveauxEmargements.stream()
                            .mapToInt(Emargement::getDuree)
                            .sum();


            double taux =
                    ens.getTauxHoraire() != null
                            ? ens.getTauxHoraire()
                            : 0.0;


            double montant =
                    totalHeures * taux;


            // ----------------------------------------------------
            // CRÉATION DU PAIEMENT
            // ----------------------------------------------------

            PaiementEnseignant paiement =
                    PaiementEnseignant.builder()
                            .enseignant(ens)
                            .periodeDebut(debut)
                            .periodeFin(fin)
                            .totalHeures(totalHeures)
                            .tauxHoraire(taux)
                            .montant(montant)
                            .statut(
                                    PaiementEnseignant.StatutPaiement
                                            .EN_ATTENTE
                            )
                            .anneeScolaireId(anneeId)
                            .emargements(
                                    new ArrayList<>(
                                            nouveauxEmargements
                                    )
                            )
                            .build();


            paiementRepo.save(paiement);

            resultats.add(
                    toDTO(paiement)
            );
        }

        return resultats;
    }


    // ============================================================
    // RÉCUPÉRER LES ÉMARGEMENTS DÉJÀ UTILISÉS
    // ============================================================

    private Set<Long> getEmargementIdsDejaUtilises(
            Long enseignantId,
            Long anneeId) {

        List<PaiementEnseignant> paiements =
                paiementRepo
                        .findByEnseignant_IdAndAnneeScolaireId(
                                enseignantId,
                                anneeId
                        );

        Set<Long> ids = new HashSet<>();

        for (PaiementEnseignant paiement : paiements) {

            if (paiement.getEmargements() == null) {
                continue;
            }

            for (Emargement emargement :
                    paiement.getEmargements()) {

                if (emargement.getId() != null) {
                    ids.add(emargement.getId());
                }
            }
        }

        return ids;
    }


    // ============================================================
    // MARQUER PAYÉ
    // ============================================================

    @Transactional
    public PaiementEnseignantDTO marquerPaye(
            Long paiementId) {

        PaiementEnseignant paiement =
                paiementRepo.findById(paiementId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Paiement enseignant introuvable"
                                )
                        );


        // Déjà payé
        if (paiement.getStatut() ==
                PaiementEnseignant.StatutPaiement.PAYE) {

            return toDTO(paiement);
        }


        paiement.setStatut(
                PaiementEnseignant.StatutPaiement.PAYE
        );

        paiement.setDatePaiement(
                LocalDate.now()
        );


        PaiementEnseignant paiementSauvegarde =
                paiementRepo.save(paiement);


        // ----------------------------------------------------
        // OPÉRATION COMPTABLE
        // ----------------------------------------------------

        operationComptableService
                .creerDepenseDepuisPaiementEnseignant(
                        paiementSauvegarde
                );


        return toDTO(paiementSauvegarde);
    }


    // ============================================================
    // LISTER
    // ============================================================

    public List<PaiementEnseignantDTO> listerPaiements(
            Long anneeId) {

        return paiementRepo
                .findByAnneeScolaireId(anneeId)
                .stream()
                .map(this::toDTO)
                .toList();
    }


    // ============================================================
    // DTO
    // ============================================================

    private PaiementEnseignantDTO toDTO(
            PaiementEnseignant p) {

        return PaiementEnseignantDTO.builder()
                .id(p.getId())
                .enseignantId(
                        p.getEnseignant().getId()
                )
                .enseignantNom(
                        p.getEnseignant().getNom()
                )
                .enseignantPrenom(
                        p.getEnseignant().getPrenom()
                )
                .periodeDebut(
                        p.getPeriodeDebut()
                )
                .periodeFin(
                        p.getPeriodeFin()
                )
                .totalHeures(
                        p.getTotalHeures()
                )
                .tauxHoraire(
                        p.getTauxHoraire()
                )
                .montant(
                        p.getMontant()
                )
                .statut(
                        p.getStatut().name()
                )
                .datePaiement(
                        p.getDatePaiement()
                )
                .build();
    }
}