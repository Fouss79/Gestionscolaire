package com.saas.school.service;

import com.saas.school.dto.EmargementResumeDTO;
import com.saas.school.dto.PaiementEnseignantDTO;
import com.saas.school.dto.RapportPaiementEnseignantDTO;
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

    // Emargement::getDuree est exprimé en MINUTES, alors que tauxHoraire
    // est un taux PAR HEURE. On convertit donc les minutes en heures
    // (division par 60) avant de multiplier par le taux, partout où ce
    // calcul est fait.
    private static final double MINUTES_PAR_HEURE = 60.0;


    // ============================================================
    // PRÉVISUALISATION
    // ============================================================

    public List<PaiementEnseignantDTO> previsualiserTous(
            Long ecoleId,
            LocalDate debut,
            LocalDate fin,
            Long anneeId) {

        // =========================================================
        // RÉCUPÉRER TOUS LES ENSEIGNANTS ACTIFS DE L'ÉCOLE
        // =========================================================

        List<Enseignant> enseignants =
                enseignantRepo.findByEcoleIdAndActifTrue(ecoleId);

        List<PaiementEnseignantDTO> resultats = new ArrayList<>();

        for (Enseignant ens : enseignants) {

            if (ens.getId() == null || ens.getTypeContrat() == null) {
                continue;
            }

            boolean estVacataire =
                    ens.getTypeContrat() == Enseignant.TypeContrat.VACATAIRE;

            // =====================================================
            // SALAIRE FIXE : CDI / CDD / STAGIAIRE
            // =====================================================

            if (!estVacataire) {

                double salaireBase =
                        ens.getSalaireBase() != null
                                ? ens.getSalaireBase()
                                : 0.0;

                boolean salaireNonConfigure = salaireBase <= 0;

                boolean salaireFixeDejaGenere =
                        !salaireNonConfigure &&
                                paiementRepo.existsChevauchementSalaireFixe(
                                        ens.getId(),
                                        anneeId,
                                        debut,
                                        fin
                                );

                resultats.add(
                        PaiementEnseignantDTO.builder()
                                .enseignantId(ens.getId())
                                .enseignantNom(ens.getNom())
                                .enseignantPrenom(ens.getPrenom())
                                .periodeDebut(debut)
                                .periodeFin(fin)

                                // Pas d'émargement pour CDI / CDD / STAGIAIRE
                                .totalHeures(0)

                                .tauxHoraire(
                                        ens.getTauxHoraire() != null
                                                ? ens.getTauxHoraire()
                                                : 0.0
                                )

                                .salaireBase(salaireBase)
                                .montantHeures(0.0)
                                .montant(salaireBase)

                                .statut(
                                        salaireNonConfigure
                                                ? "SALAIRE_NON_CONFIGURE"
                                                : salaireFixeDejaGenere
                                                ? "DEJA_GENERE"
                                                : "NON_GENERE"
                                )
                                .build()
                );



                // On passe à l'enseignant suivant
                continue;
            }

            // =====================================================
            // VACATAIRE : PAIEMENT SELON LES ÉMARGEMENTS
            // =====================================================

            List<Emargement> emargements =
                    emargementRepo
                            .findByEmploiDuTemps_Enseignant_IdAndDateHeureBetweenAndEmploiDuTemps_AnneeScolaireId(
                                    ens.getId(),
                                    debut,
                                    fin,
                                    anneeId
                            );

            // =====================================================
            // ÉMARGEMENTS DÉJÀ PAYÉS / GÉNÉRÉS
            // =====================================================

            Set<Long> emargementsDejaUtilises =
                    getEmargementIdsDejaUtilises(
                            ens.getId(),
                            anneeId
                    );

            // =====================================================
            // GARDER UNIQUEMENT LES NOUVEAUX ÉMARGEMENTS
            // =====================================================

            List<Emargement> nouveauxEmargements =
                    emargements.stream()
                            .filter(em ->
                                    em.getId() != null &&
                                            !emargementsDejaUtilises.contains(
                                                    em.getId()
                                            )
                            )
                            .toList();

            // Un vacataire sans nouvel émargement
            // ne doit pas apparaître comme paiement à générer.
            if (nouveauxEmargements.isEmpty()) {
                continue;
            }

            // =====================================================
            // CALCUL DES MINUTES
            // =====================================================

            int totalHeures =
                    nouveauxEmargements.stream()
                            .mapToInt(Emargement::getDuree)
                            .sum();

            double taux =
                    ens.getTauxHoraire() != null
                            ? ens.getTauxHoraire()
                            : 0.0;

            // =====================================================
            // MINUTES -> HEURES -> MONTANT
            // =====================================================

            double montantHeures =
                    (totalHeures / MINUTES_PAR_HEURE) * taux;

            double montantTotal = montantHeures;

            // =====================================================
            // DTO VACATAIRE
            // =====================================================

            resultats.add(
                    PaiementEnseignantDTO.builder()
                            .enseignantId(ens.getId())
                            .enseignantNom(ens.getNom())
                            .enseignantPrenom(ens.getPrenom())
                            .periodeDebut(debut)
                            .periodeFin(fin)

                            // Attention : cette propriété contient
                            // toujours des MINUTES malgré son nom.
                            .totalHeures(totalHeures)

                            .tauxHoraire(taux)
                            .salaireBase(0.0)
                            .montantHeures(montantHeures)
                            .montant(montantTotal)

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
            Long ecoleId,
            LocalDate debut,
            LocalDate fin,
            Long anneeId) {

        List<Enseignant> enseignants =
                enseignantRepo.findByEcoleIdAndActifTrue(ecoleId);

        List<PaiementEnseignantDTO> resultats = new ArrayList<>();

        for (Enseignant ens : enseignants) {

            if (ens.getTypeContrat() == null) {
                continue;
            }

            boolean estVacataire =
                    ens.getTypeContrat() == Enseignant.TypeContrat.VACATAIRE;

            // ============================================================
            // 1. ENSEIGNANT À SALAIRE FIXE
            // CDI / CDD / STAGIAIRE
            // ============================================================

            if (!estVacataire) {

                Double salaireBase = ens.getSalaireBase();

                if (salaireBase == null || salaireBase <= 0) {
                    continue;
                }

                // Protection contre le double paiement
                boolean dejaGenere =
                        paiementRepo.existsChevauchementSalaireFixe(
                                ens.getId(),
                                anneeId,
                                debut,
                                fin
                        );

                if (dejaGenere) {
                    continue;
                }

                PaiementEnseignant paiement =
                        PaiementEnseignant.builder()
                                .enseignant(ens)
                                .periodeDebut(debut)
                                .periodeFin(fin)

                                // Pas d'émargement
                                .totalHeures(0)

                                .tauxHoraire(
                                        ens.getTauxHoraire() != null
                                                ? ens.getTauxHoraire()
                                                : 0.0
                                )

                                .salaireBase(salaireBase)

                                .montantHeures(0.0)

                                .montant(salaireBase)

                                .statut(
                                        PaiementEnseignant.StatutPaiement
                                                .EN_ATTENTE
                                )

                                .anneeScolaireId(anneeId)

                                .emargements(new ArrayList<>())

                                .build();

                paiementRepo.save(paiement);

                resultats.add(toDTO(paiement));

                continue;
            }

            // ============================================================
            // 2. VACATAIRE
            // Paiement uniquement selon les émargements
            // ============================================================

            List<Emargement> emargements =
                    emargementRepo
                            .findByEmploiDuTemps_Enseignant_IdAndDateHeureBetweenAndEmploiDuTemps_AnneeScolaireId(
                                    ens.getId(),
                                    debut,
                                    fin,
                                    anneeId
                            );

            // Émargements déjà utilisés dans des paiements précédents
            Set<Long> emargementsDejaUtilises =
                    getEmargementIdsDejaUtilises(
                            ens.getId(),
                            anneeId
                    );

            // Seulement les nouvelles heures
            List<Emargement> nouveauxEmargements =
                    emargements.stream()
                            .filter(em ->
                                    em.getId() != null &&
                                            !emargementsDejaUtilises.contains(
                                                    em.getId()
                                            )
                            )
                            .toList();

            // Aucun émargement = aucun paiement vacataire
            if (nouveauxEmargements.isEmpty()) {
                continue;
            }

            int totalMinutes =
                    nouveauxEmargements.stream()
                            .mapToInt(Emargement::getDuree)
                            .sum();

            double taux =
                    ens.getTauxHoraire() != null
                            ? ens.getTauxHoraire()
                            : 0.0;

            double montantHeures =
                    (totalMinutes / MINUTES_PAR_HEURE) * taux;

            PaiementEnseignant paiement =
                    PaiementEnseignant.builder()
                            .enseignant(ens)
                            .periodeDebut(debut)
                            .periodeFin(fin)
                            .totalHeures(totalMinutes)
                            .tauxHoraire(taux)
                            .salaireBase(0.0)
                            .montantHeures(montantHeures)
                            .montant(montantHeures)
                            .statut(
                                    PaiementEnseignant.StatutPaiement
                                            .EN_ATTENTE
                            )
                            .anneeScolaireId(anneeId)
                            .emargements(
                                    new ArrayList<>(nouveauxEmargements)
                            )
                            .build();

            paiementRepo.save(paiement);

            resultats.add(toDTO(paiement));
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
    public RapportPaiementEnseignantDTO rapportEnseignant(
            Long enseignantId,
            Long anneeId
    ) {

        Enseignant enseignant = enseignantRepo.findById(enseignantId)
                .orElseThrow(() ->
                        new RuntimeException("Enseignant introuvable")
                );

        List<PaiementEnseignant> paiements =
                paiementRepo.findByEnseignant_IdAndAnneeScolaireId(
                        enseignantId,
                        anneeId
                );

        int totalHeures = paiements.stream()
                .mapToInt(PaiementEnseignant::getTotalHeures)
                .sum();

        double totalMontantHeures = paiements.stream()
                .mapToDouble(PaiementEnseignant::getMontantHeures)
                .sum();

        double totalSalaireBase = paiements.stream()
                .mapToDouble(PaiementEnseignant::getSalaireBase)
                .sum();

        double totalMontant = paiements.stream()
                .mapToDouble(PaiementEnseignant::getMontant)
                .sum();
        double totalPaye = paiements.stream()
                .filter(p ->
                        p.getStatut() ==
                                PaiementEnseignant.StatutPaiement.PAYE
                )
                .mapToDouble(p -> p.getMontant() != null
                        ? p.getMontant()
                        : 0.0)
                .sum();

        double totalEnAttente = paiements.stream()
                .filter(p ->
                        p.getStatut() ==
                                PaiementEnseignant.StatutPaiement.EN_ATTENTE
                )
                .mapToDouble(p -> p.getMontant() != null
                        ? p.getMontant()
                        : 0.0)
                .sum();

        List<RapportPaiementEnseignantDTO.PaiementLigneDTO> lignes =
                paiements.stream()
                        .map(p ->
                                RapportPaiementEnseignantDTO.PaiementLigneDTO
                                        .builder()
                                        .id(p.getId())
                                        .periodeDebut(p.getPeriodeDebut())
                                        .periodeFin(p.getPeriodeFin())
                                        .totalHeures(p.getTotalHeures())
                                        .tauxHoraire(p.getTauxHoraire())
                                        .salaireBase(p.getSalaireBase())
                                        .montantHeures(p.getMontantHeures())
                                        .montant(p.getMontant())
                                        .statut(
                                                p.getStatut() != null
                                                        ? p.getStatut().name()
                                                        : null
                                        )
                                        .datePaiement(p.getDatePaiement())
                                        .build()
                        )
                        .toList();
        return RapportPaiementEnseignantDTO.builder()
                .enseignantId(enseignant.getId())
                .enseignantNom(enseignant.getNom())
                .enseignantPrenom(enseignant.getPrenom())
                .matricule(enseignant.getMatricule())
                .typeContrat(
                        enseignant.getTypeContrat() != null
                                ? enseignant.getTypeContrat().name()
                                : null
                )
                .totalHeures(totalHeures)
                .totalMontantHeures(totalMontantHeures)
                .totalSalaireBase(totalSalaireBase)
                .totalMontant(totalMontant)
                .totalPaye(totalPaye)
                .totalEnAttente(totalEnAttente)
                .paiements(lignes)
                .build();
    }
    public PaiementEnseignant getById(Long id) {
        return paiementRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Paiement enseignant introuvable"));
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
                .salaireBase(
                        p.getSalaireBase()
                )
                .montantHeures(
                        p.getMontantHeures()
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