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
            // CALCUL DU SALAIRE — selon le type de contrat :
            //   - VACATAIRE  : payé uniquement aux heures émargées
            //   - CDI/CDD/STAGIAIRE : salaire fixe (salaireBase), les
            //     heures émargées ne sont pas payées à l'heure ici
            // ----------------------------------------------------

            boolean estVacataire =
                    ens.getTypeContrat() == Enseignant.TypeContrat.VACATAIRE;

            int totalHeures = nouveauxEmargements.stream()
                    .mapToInt(Emargement::getDuree)
                    .sum();

            double taux =
                    ens.getTauxHoraire() != null
                            ? ens.getTauxHoraire()
                            : 0.0;

            double salaireBase =
                    !estVacataire && ens.getSalaireBase() != null
                            ? ens.getSalaireBase()
                            : 0.0;

            double montantHeures =
                    estVacataire ? totalHeures * taux : 0.0;

            double montantTotal = salaireBase + montantHeures;

            boolean salaireFixeDejaGenere =
                    !estVacataire && salaireBase > 0
                            && paiementRepo.existsChevauchementSalaireFixe(
                            r.getEnseignantId(), anneeId, debut, fin
                    );

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
                            .salaireBase(salaireBase)
                            .montantHeures(montantHeures)
                            .montant(montantTotal)
                            .statut(salaireFixeDejaGenere ? "DEJA_GENERE" : "NON_GENERE")
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
            // AUCUNE NOUVELLE HEURE — pour un VACATAIRE, pas d'heure
            // émargée = rien à payer, on saute. Pour un CDI/CDD/
            // STAGIAIRE, le salaire fixe est dû indépendamment des
            // émargements : on ne saute que s'il n'y a ni heures ni
            // salaire de base.
            // ----------------------------------------------------

            boolean estVacataire =
                    ens.getTypeContrat() == Enseignant.TypeContrat.VACATAIRE;

            double salaireBase =
                    !estVacataire && ens.getSalaireBase() != null
                            ? ens.getSalaireBase()
                            : 0.0;

            if (estVacataire && nouveauxEmargements.isEmpty()) {
                continue;
            }

            if (!estVacataire && salaireBase <= 0) {
                continue;
            }

            // ----------------------------------------------------
            // PROTECTION ANTI-DOUBLON — un salaire fixe ne doit être
            // généré qu'une seule fois pour une même période exacte
            // (contrairement aux heures de vacataire, déjà protégées
            // par emargementsDejaUtilises). Sans ce garde-fou, relancer
            // genererPaiements deux fois sur la même période créerait
            // deux paiements du même salaire fixe.
            // ----------------------------------------------------

            if (!estVacataire && paiementRepo.existsChevauchementSalaireFixe(
                    r.getEnseignantId(), anneeId, debut, fin
            )) {
                continue;
            }


            // ----------------------------------------------------
            // CALCUL HEURES + SALAIRE
            // ----------------------------------------------------

            int totalHeures =
                    nouveauxEmargements.stream()
                            .mapToInt(Emargement::getDuree)
                            .sum();


            double taux =
                    ens.getTauxHoraire() != null
                            ? ens.getTauxHoraire()
                            : 0.0;


            double montantHeures =
                    estVacataire ? totalHeures * taux : 0.0;

            double montantTotal = salaireBase + montantHeures;





            System.out.println("========== CALCUL PAIEMENT ==========");
            System.out.println("Enseignant : " + ens.getPrenom() + " " + ens.getNom());
            System.out.println("Type contrat : " + ens.getTypeContrat());
            System.out.println("Est vacataire : " + estVacataire);
            System.out.println("Nombre émargements : " + nouveauxEmargements.size());
            System.out.println("Total heures : " + totalHeures);
            System.out.println("Taux horaire : " + taux);
            System.out.println("Montant heures : " + montantHeures);
            System.out.println("=====================================");
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
                            .salaireBase(salaireBase)
                            .montantHeures(montantHeures)
                            .montant(montantTotal)
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