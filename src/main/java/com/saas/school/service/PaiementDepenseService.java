package com.saas.school.service;

import com.saas.school.dto.PaiementDepenseRequestDTO;
import com.saas.school.dto.PaiementDepenseResponseDTO;
import com.saas.school.entity.Depense;
import com.saas.school.entity.PaiementDepense;

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
    private final OperationComptableService operationComptableService;

    @Transactional
    public PaiementDepenseResponseDTO enregistrerPaiement(PaiementDepenseRequestDTO dto) {

        if (dto.getDepenseId() == null) {
            throw new RuntimeException("La dépense est obligatoire.");
        }

        if (dto.getMontant() == null || dto.getMontant() <= 0) {
            throw new RuntimeException("Le montant doit être supérieur à zéro.");
        }

        Depense depense = depenseRepository.findById(dto.getDepenseId())
                .orElseThrow(() -> new RuntimeException("Dépense introuvable."));

        double montantPayeActuel = depense.getMontantPaye() != null ? depense.getMontantPaye() : 0.0;
        double montantTotal = depense.getMontantTotal() != null ? depense.getMontantTotal() : 0.0;
        double resteActuel = montantTotal - montantPayeActuel;

        if (dto.getMontant() > resteActuel) {
            throw new RuntimeException(
                    "Le montant (" + dto.getMontant() + ") dépasse le reste à payer (" + resteActuel + ")."
            );
        }

        // ===== Création du versement =====
        PaiementDepense paiement = new PaiementDepense();

        paiement.setDepense(depense);
        paiement.setMontant(dto.getMontant());
        paiement.setModePaiement(dto.getModePaiement());

        if ("CASH".equalsIgnoreCase(dto.getModePaiement())) {
            paiement.setReference(genererReferenceCash());
        } else {
            if (dto.getReference() == null || dto.getReference().isBlank()) {
                throw new RuntimeException("La référence est obligatoire pour ce mode de paiement.");
            }
            paiement.setReference(dto.getReference().trim());
        }

        paiement.setDatePaiement(LocalDateTime.now());

        // ===== Mise à jour de la dépense =====
        double nouveauMontantPaye = montantPayeActuel + dto.getMontant();
        double nouveauReste = montantTotal - nouveauMontantPaye;

        depense.setMontantPaye(nouveauMontantPaye);
        depense.setResteAPayer(Math.max(0.0, nouveauReste));

        if (nouveauReste <= 0) {
            depense.setResteAPayer(0.0);
            depense.setStatutPaiement(StatutPaiement.PAYE);
        } else if (nouveauMontantPaye > 0) {
            depense.setStatutPaiement(StatutPaiement.PARTIEL);
        } else {
            depense.setStatutPaiement(StatutPaiement.NON_PAYE);
        }

        depenseRepository.save(depense);

        PaiementDepense saved = paiementDepenseRepository.save(paiement);

        // ===== Opération comptable (recette côté paiement scolarité,
        //       ici une dépense) créée pour CE versement précis =====
        operationComptableService.creerDepenseDepuisPaiement(saved, depense.getEcole());

        return mapToDto(saved);
    }

    public List<PaiementDepenseResponseDTO> getByDepense(Long depenseId) {
        return paiementDepenseRepository
                .findByDepense_IdOrderByDatePaiementDesc(depenseId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<PaiementDepenseResponseDTO> getByEcole(Long ecoleId) {
        return paiementDepenseRepository
                .findByDepense_Ecole_IdOrderByDatePaiementDesc(ecoleId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private String genererReferenceCash() {
        return "DEP-" + System.currentTimeMillis();
    }

    private PaiementDepenseResponseDTO mapToDto(PaiementDepense p) {

        Depense depense = p.getDepense();

        PaiementDepenseResponseDTO dto = new PaiementDepenseResponseDTO();

        dto.setId(p.getId());
        dto.setDepenseId(depense.getId());
        dto.setDepenseLibelle(depense.getLibelle());

        dto.setMontant(p.getMontant());
        dto.setModePaiement(p.getModePaiement());
        dto.setReference(p.getReference());
        dto.setDatePaiement(p.getDatePaiement());

        dto.setMontantTotal(depense.getMontantTotal());
        dto.setMontantPayeTotal(depense.getMontantPaye());
        dto.setResteAPayer(depense.getResteAPayer());
        dto.setStatutPaiement(depense.getStatutPaiement() != null ? depense.getStatutPaiement().name() : null);

        return dto;
    }
}