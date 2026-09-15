package com.saas.school.service;

import com.saas.school.dto.PaiementDepenseRequestDTO;
import com.saas.school.dto.PaiementDepenseResponseDTO;
import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Depense;
import com.saas.school.entity.PaiementDepense;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.DepenseRepository;
import com.saas.school.repository.PaiementDepenseRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaiementDepenseService {

    private final PaiementDepenseRepository paiementDepenseRepository;
    private final DepenseRepository depenseRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;
    private final OperationComptableService operationComptableService;


    // ============================================================
    // ENREGISTRER UN PAIEMENT
    // ============================================================

    @Transactional
    public PaiementDepenseResponseDTO enregistrerPaiement(
            PaiementDepenseRequestDTO dto
    ) {

        // =========================
        // VALIDATION
        // =========================

        if (dto == null) {
            throw new RuntimeException("Les données du paiement sont obligatoires.");
        }

        if (dto.getDepenseId() == null) {
            throw new RuntimeException("La dépense est obligatoire.");
        }

        if (dto.getAnneeId() == null) {
            throw new RuntimeException("L'année scolaire est obligatoire.");
        }

        if (dto.getMontant() == null || dto.getMontant() <= 0) {
            throw new RuntimeException(
                    "Le montant doit être supérieur à zéro."
            );
        }


        // =========================
        // RÉCUPÉRER LA DÉPENSE
        // =========================

        Depense depense = depenseRepository.findById(dto.getDepenseId())
                .orElseThrow(() ->
                        new RuntimeException("Dépense introuvable.")
                );


        // =========================
        // VÉRIFIER L'ÉCOLE
        // =========================

        if (depense.getEcole() == null) {
            throw new RuntimeException(
                    "La dépense n'est associée à aucune école."
            );
        }


        // =========================
        // RÉCUPÉRER L'ANNÉE
        // =========================

        AnneeScolaire annee = anneeScolaireRepository
                .findById(dto.getAnneeId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Année scolaire introuvable."
                        )
                );


        // =========================
        // VÉRIFIER L'ÉCOLE DE L'ANNÉE
        // =========================

        if (annee.getEcole() == null) {
            throw new RuntimeException(
                    "L'année scolaire n'est associée à aucune école."
            );
        }

        if (!annee.getEcole().getId()
                .equals(depense.getEcole().getId())) {

            throw new RuntimeException(
                    "Cette année scolaire n'appartient pas à l'école de la dépense."
            );
        }


        // ============================================================
        // VÉRIFIER L'ANNÉE DE LA DÉPENSE
        // ============================================================

        if (depense.getAnneeScolaire() != null
                && !depense.getAnneeScolaire().getId()
                .equals(annee.getId())) {

            throw new RuntimeException(
                    "L'année scolaire du paiement ne correspond pas " +
                            "à celle de la dépense."
            );
        }


        // ============================================================
        // CALCUL DU RESTE
        // ============================================================

        double montantPayeActuel =
                depense.getMontantPaye() != null
                        ? depense.getMontantPaye()
                        : 0.0;

        double montantTotal =
                depense.getMontantTotal() != null
                        ? depense.getMontantTotal()
                        : 0.0;

        double resteActuel =
                Math.max(0.0, montantTotal - montantPayeActuel);


        if (resteActuel <= 0) {
            throw new RuntimeException(
                    "Cette dépense est déjà entièrement payée."
            );
        }


        if (dto.getMontant() > resteActuel) {
            throw new RuntimeException(
                    "Le montant (" + dto.getMontant()
                            + ") dépasse le reste à payer ("
                            + resteActuel + ")."
            );
        }


        // ============================================================
        // CRÉATION DU PAIEMENT
        // ============================================================

        PaiementDepense paiement = new PaiementDepense();

        paiement.setDepense(depense);

        paiement.setAnneeScolaire(annee);

        paiement.setMontant(dto.getMontant());

        paiement.setModePaiement(dto.getModePaiement());


        // =========================
        // RÉFÉRENCE
        // =========================

        if ("CASH".equalsIgnoreCase(dto.getModePaiement())) {

            paiement.setReference(
                    genererReferenceCash()
            );

        } else {

            if (dto.getReference() == null
                    || dto.getReference().isBlank()) {

                throw new RuntimeException(
                        "La référence est obligatoire pour ce mode de paiement."
                );
            }

            paiement.setReference(
                    dto.getReference().trim()
            );
        }


        paiement.setDatePaiement(
                LocalDateTime.now()
        );


        // ============================================================
        // MISE À JOUR DE LA DÉPENSE
        // ============================================================

        double nouveauMontantPaye =
                montantPayeActuel + dto.getMontant();

        double nouveauReste =
                Math.max(
                        0.0,
                        montantTotal - nouveauMontantPaye
                );


        depense.setMontantPaye(
                nouveauMontantPaye
        );

        depense.setResteAPayer(
                nouveauReste
        );


        // =========================
        // STATUT
        // =========================

        if (nouveauReste <= 0) {

            depense.setStatutPaiement(
                    StatutPaiement.PAYE
            );

        } else if (nouveauMontantPaye > 0) {

            depense.setStatutPaiement(
                    StatutPaiement.PARTIEL
            );

        } else {

            depense.setStatutPaiement(
                    StatutPaiement.NON_PAYE
            );
        }


        // =========================
        // SAUVEGARDE DÉPENSE
        // =========================

        depenseRepository.save(depense);


        // =========================
        // SAUVEGARDE PAIEMENT
        // =========================

        PaiementDepense saved =
                paiementDepenseRepository.save(paiement);


        // ============================================================
        // OPÉRATION COMPTABLE
        // ============================================================

        operationComptableService.creerDepenseDepuisPaiement(
                saved,
                depense.getEcole()
        );


        // =========================
        // RETOUR
        // =========================

        return mapToDto(saved);
    }


    // ============================================================
    // PAIEMENTS D'UNE DÉPENSE
    // ============================================================

    public List<PaiementDepenseResponseDTO> getByDepense(
            Long depenseId
    ) {

        return paiementDepenseRepository
                .findByDepense_IdOrderByDatePaiementDesc(
                        depenseId
                )
                .stream()
                .map(this::mapToDto)
                .toList();
    }


    // ============================================================
    // PAIEMENTS D'UNE ÉCOLE POUR UNE ANNÉE
    // ============================================================

    public List<PaiementDepenseResponseDTO> getByEcoleAndAnnee(
            Long ecoleId,
            Long anneeId
    ) {

        if (ecoleId == null) {
            throw new RuntimeException("L'école est obligatoire.");
        }

        if (anneeId == null) {
            throw new RuntimeException("L'année scolaire est obligatoire.");
        }

        AnneeScolaire annee = anneeScolaireRepository
                .findById(anneeId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Année scolaire introuvable."
                        )
                );

        if (annee.getEcole() == null
                || !annee.getEcole().getId().equals(ecoleId)) {

            throw new RuntimeException(
                    "Cette année scolaire n'appartient pas à cette école."
            );
        }

        return paiementDepenseRepository
                .findByDepense_Ecole_IdAndAnneeScolaire_IdOrderByDatePaiementDesc(
                        ecoleId,
                        anneeId
                )
                .stream()
                .map(this::mapToDto)
                .toList();
    }


    // ============================================================
    // RÉFÉRENCE CASH
    // ============================================================

    private String genererReferenceCash() {

        return "DEP-" + System.currentTimeMillis();
    }


    // ============================================================
    // MAPPING DTO
    // ============================================================

    private PaiementDepenseResponseDTO mapToDto(
            PaiementDepense p
    ) {

        PaiementDepenseResponseDTO dto =
                new PaiementDepenseResponseDTO();

        dto.setId(p.getId());


        // =========================
        // DÉPENSE
        // =========================

        if (p.getDepense() != null) {

            Depense depense = p.getDepense();

            dto.setDepenseId(
                    depense.getId()
            );

            dto.setDepenseLibelle(
                    depense.getLibelle()
            );

            dto.setMontantTotal(
                    depense.getMontantTotal()
            );

            dto.setMontantPayeTotal(
                    depense.getMontantPaye()
            );

            dto.setResteAPayer(
                    depense.getResteAPayer()
            );

            dto.setStatutPaiement(
                    depense.getStatutPaiement() != null
                            ? depense.getStatutPaiement().name()
                            : null
            );
        }


        // =========================
        // PAIEMENT
        // =========================

        dto.setMontant(
                p.getMontant()
        );

        dto.setModePaiement(
                p.getModePaiement()
        );

        dto.setReference(
                p.getReference()
        );

        dto.setDatePaiement(
                p.getDatePaiement()
        );


        // =========================
        // ANNÉE SCOLAIRE
        // =========================

        if (p.getAnneeScolaire() != null) {

            dto.setAnneeId(
                    p.getAnneeScolaire().getId()
            );

            dto.setAnneeLibelle(
                    p.getAnneeScolaire().getNom()
            );
        }

        return dto;
    }
}