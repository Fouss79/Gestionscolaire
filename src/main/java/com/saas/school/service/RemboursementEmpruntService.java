package com.saas.school.service;

import com.saas.school.dto.RemboursementEmpruntRequestDTO;
import com.saas.school.dto.RemboursementEmpruntResponseDTO;
import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Emprunt;
import com.saas.school.entity.RemboursementEmprunt;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.EmpruntRepository;
import com.saas.school.repository.RemboursementEmpruntRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RemboursementEmpruntService {

    private final RemboursementEmpruntRepository remboursementEmpruntRepository;
    private final EmpruntRepository empruntRepository;
    private final OperationComptableService operationComptableService;
    private final AnneeScolaireRepository anneeScolaireRepository;


    // =========================================================
    // ENREGISTRER UN REMBOURSEMENT
    // =========================================================

    @Transactional
    public RemboursementEmpruntResponseDTO enregistrerRemboursement(
            RemboursementEmpruntRequestDTO dto
    ) {

        if (dto == null) {
            throw new IllegalArgumentException(
                    "Les données du remboursement sont obligatoires."
            );
        }

        if (dto.getEmpruntId() == null) {
            throw new IllegalArgumentException(
                    "L'emprunt est obligatoire."
            );
        }

        if (dto.getMontant() == null || dto.getMontant() <= 0) {
            throw new IllegalArgumentException(
                    "Le montant doit être supérieur à zéro."
            );
        }

        // =====================================================
        // RÉCUPÉRATION DE L'EMPRUNT
        // =====================================================

        Emprunt emprunt = empruntRepository
                .findById(dto.getEmpruntId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Emprunt introuvable."
                        )
                );

        // =====================================================
        // VÉRIFICATION DE L'ANNÉE SCOLAIRE
        // =====================================================

        AnneeScolaire anneeScolaire =
                emprunt.getAnneeScolaire();

        if (anneeScolaire == null
                || anneeScolaire.getId() == null) {

            throw new IllegalStateException(
                    "Cet emprunt n'est associé à aucune année scolaire."
            );
        }

        if (emprunt.getEcole() == null
                || emprunt.getEcole().getId() == null) {

            throw new IllegalStateException(
                    "Cet emprunt n'est associé à aucune école."
            );
        }

        if (anneeScolaire.getEcole() == null
                || anneeScolaire.getEcole().getId() == null
                || !anneeScolaire.getEcole()
                .getId()
                .equals(emprunt.getEcole().getId())) {

            throw new IllegalStateException(
                    "L'année scolaire de l'emprunt n'appartient pas à son école."
            );
        }

        // =====================================================
        // DATE DU REMBOURSEMENT
        // =====================================================

        LocalDate dateRemboursement =
                dto.getDateRemboursement() != null
                        ? dto.getDateRemboursement()
                        : LocalDate.now();

        if (emprunt.getDateEmprunt() != null
                && dateRemboursement.isBefore(
                emprunt.getDateEmprunt().toLocalDate()
        )) {

            throw new IllegalArgumentException(
                    "La date du remboursement ne peut pas être antérieure à la date de l'emprunt."
            );
        }

        // =====================================================
        // CALCUL DU RESTE À REMBOURSER
        // =====================================================

        double montantRembourseActuel =
                emprunt.getMontantRembourse() != null
                        ? emprunt.getMontantRembourse()
                        : 0.0;

        double montantARembourser =
                emprunt.getMontantARembourser() != null
                        ? emprunt.getMontantARembourser()
                        : 0.0;

        double resteActuel =
                Math.max(
                        0.0,
                        montantARembourser
                                - montantRembourseActuel
                );

        if (resteActuel <= 0) {
            throw new IllegalArgumentException(
                    "Cet emprunt est déjà entièrement remboursé."
            );
        }

        if (dto.getMontant() > resteActuel) {
            throw new IllegalArgumentException(
                    "Le montant (" + dto.getMontant()
                            + ") dépasse le reste à rembourser ("
                            + resteActuel + ")."
            );
        }

        // =====================================================
        // CRÉATION DU REMBOURSEMENT
        // =====================================================

        RemboursementEmprunt remboursement =
                new RemboursementEmprunt();

        remboursement.setEmprunt(emprunt);
        remboursement.setAnneeScolaire(anneeScolaire);
        remboursement.setMontant(dto.getMontant());
        remboursement.setModePaiement(dto.getModePaiement());
        // =====================================================
        // RÉFÉRENCE
        // =====================================================

        if ("CASH".equalsIgnoreCase(dto.getModePaiement())) {

            remboursement.setReference(
                    genererReferenceCash()
            );

        } else {

            if (dto.getReference() == null
                    || dto.getReference().isBlank()) {

                throw new IllegalArgumentException(
                        "La référence est obligatoire pour ce mode de paiement."
                );
            }

            remboursement.setReference(
                    dto.getReference().trim()
            );
        }

        remboursement.setDateRemboursement(
                dateRemboursement.atStartOfDay()
        );

        // =====================================================
        // MISE À JOUR DE L'EMPRUNT
        // =====================================================

        double nouveauMontantRembourse =
                montantRembourseActuel
                        + dto.getMontant();

        double nouveauReste =
                montantARembourser
                        - nouveauMontantRembourse;

        emprunt.setMontantRembourse(
                nouveauMontantRembourse
        );

        emprunt.setResteAPayer(
                Math.max(0.0, nouveauReste)
        );

        if (nouveauReste <= 0) {

            emprunt.setResteAPayer(0.0);

            emprunt.setStatutPaiement(
                    StatutPaiement.PAYE
            );

        } else if (nouveauMontantRembourse > 0) {

            emprunt.setStatutPaiement(
                    StatutPaiement.PARTIEL
            );

        } else {

            emprunt.setStatutPaiement(
                    StatutPaiement.NON_PAYE
            );
        }

        // =====================================================
        // SAUVEGARDE DE L'EMPRUNT
        // =====================================================

        empruntRepository.save(emprunt);

        // =====================================================
        // SAUVEGARDE DU REMBOURSEMENT
        // =====================================================

        RemboursementEmprunt saved =
                remboursementEmpruntRepository
                        .save(remboursement);

        // =====================================================
        // OPÉRATION COMPTABLE
        // =====================================================

        operationComptableService
                .creerDepenseDepuisRemboursement(
                        saved,
                        emprunt.getEcole()
                );

        return mapToDto(saved);
    }


    // =========================================================
    // REMBOURSEMENTS D'UN EMPRUNT
    // =========================================================

    @Transactional
    public List<RemboursementEmpruntResponseDTO> getByEmprunt(
            Long empruntId
    ) {

        if (empruntId == null) {
            throw new IllegalArgumentException(
                    "L'emprunt est obligatoire."
            );
        }

        return remboursementEmpruntRepository
                .findByEmprunt_IdOrderByDateRemboursementDesc(
                        empruntId
                )
                .stream()
                .map(this::mapToDto)
                .toList();
    }


    // =========================================================
    // REMBOURSEMENTS D'UNE ÉCOLE + ANNÉE SCOLAIRE
    // =========================================================

    @Transactional
    public List<RemboursementEmpruntResponseDTO> getByEcole(
            Long ecoleId,
            Long anneeId
    ) {

        if (ecoleId == null) {
            throw new IllegalArgumentException(
                    "L'école est obligatoire."
            );
        }

        if (anneeId == null) {
            throw new IllegalArgumentException(
                    "L'année scolaire est obligatoire."
            );
        }

        // Vérifier que l'année existe
        AnneeScolaire anneeScolaire =
                anneeScolaireRepository
                        .findById(anneeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Année scolaire introuvable."
                                )
                        );

        // Vérifier que l'année appartient à l'école
        if (anneeScolaire.getEcole() == null
                || anneeScolaire.getEcole().getId() == null
                || !anneeScolaire.getEcole()
                .getId()
                .equals(ecoleId)) {

            throw new IllegalArgumentException(
                    "Cette année scolaire n'appartient pas à cette école."
            );
        }

        return remboursementEmpruntRepository
                .findByEmprunt_Ecole_IdAndEmprunt_AnneeScolaire_IdOrderByDateRemboursementDesc(
                        ecoleId,
                        anneeId
                )
                .stream()
                .map(this::mapToDto)
                .toList();
    }


    // =========================================================
    // ALIAS
    // =========================================================

    public List<RemboursementEmpruntResponseDTO> findByEcole(
            Long ecoleId,
            Long anneeId
    ) {
        return getByEcole(ecoleId, anneeId);
    }


    // =========================================================
    // RÉFÉRENCE CASH
    // =========================================================

    private String genererReferenceCash() {
        return "EMP-" + System.currentTimeMillis();
    }


    // =========================================================
    // ENTITY -> DTO
    // =========================================================

    private RemboursementEmpruntResponseDTO mapToDto(
            RemboursementEmprunt r
    ) {

        if (r == null) {
            return null;
        }

        Emprunt emprunt = r.getEmprunt();

        RemboursementEmpruntResponseDTO dto =
                new RemboursementEmpruntResponseDTO();

        dto.setId(r.getId());

        if (emprunt != null) {

            dto.setEmpruntId(
                    emprunt.getId()
            );

            dto.setEmpruntLibelle(
                    emprunt.getLibelle()
            );

            dto.setMontantTotal(
                    emprunt.getMontantARembourser()
            );

            dto.setMontantRembourseTotal(
                    emprunt.getMontantRembourse()
            );

            dto.setResteAPayer(
                    emprunt.getResteAPayer()
            );

            dto.setStatutPaiement(
                    emprunt.getStatutPaiement() != null
                            ? emprunt.getStatutPaiement().name()
                            : null
            );
        }

        dto.setMontant(
                r.getMontant()
        );

        dto.setModePaiement(
                r.getModePaiement()
        );

        dto.setReference(
                r.getReference()
        );

        dto.setDateRemboursement(
                r.getDateRemboursement()
        );

        return dto;
    }
}