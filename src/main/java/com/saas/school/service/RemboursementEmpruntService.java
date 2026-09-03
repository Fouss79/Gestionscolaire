package com.saas.school.service;

import com.saas.school.dto.RemboursementEmpruntRequestDTO;
import com.saas.school.dto.RemboursementEmpruntResponseDTO;
import com.saas.school.entity.Emprunt;
import com.saas.school.entity.RemboursementEmprunt;

import com.saas.school.repository.EmpruntRepository;
import com.saas.school.repository.RemboursementEmpruntRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RemboursementEmpruntService {

    private final RemboursementEmpruntRepository remboursementEmpruntRepository;
    private final EmpruntRepository empruntRepository;
    private final OperationComptableService operationComptableService;

    @Transactional
    public RemboursementEmpruntResponseDTO enregistrerRemboursement(RemboursementEmpruntRequestDTO dto) {

        if (dto.getEmpruntId() == null) {
            throw new RuntimeException("L'emprunt est obligatoire.");
        }

        if (dto.getMontant() == null || dto.getMontant() <= 0) {
            throw new RuntimeException("Le montant doit être supérieur à zéro.");
        }

        Emprunt emprunt = empruntRepository.findById(dto.getEmpruntId())
                .orElseThrow(() -> new RuntimeException("Emprunt introuvable."));

        double montantRembourseActuel = emprunt.getMontantRembourse() != null ? emprunt.getMontantRembourse() : 0.0;
        double montantARembourser = emprunt.getMontantARembourser() != null ? emprunt.getMontantARembourser() : 0.0;
        double resteActuel = montantARembourser - montantRembourseActuel;

        if (dto.getMontant() > resteActuel) {
            throw new RuntimeException(
                    "Le montant (" + dto.getMontant() + ") dépasse le reste à rembourser (" + resteActuel + ")."
            );
        }

        // ===== Création du remboursement =====
        RemboursementEmprunt remboursement = new RemboursementEmprunt();

        remboursement.setEmprunt(emprunt);
        remboursement.setMontant(dto.getMontant());
        remboursement.setModePaiement(dto.getModePaiement());

        if ("CASH".equalsIgnoreCase(dto.getModePaiement())) {
            remboursement.setReference(genererReferenceCash());
        } else {
            if (dto.getReference() == null || dto.getReference().isBlank()) {
                throw new RuntimeException("La référence est obligatoire pour ce mode de paiement.");
            }
            remboursement.setReference(dto.getReference().trim());
        }

        remboursement.setDateRemboursement(LocalDateTime.now());

        // ===== Mise à jour de l'emprunt =====
        double nouveauMontantRembourse = montantRembourseActuel + dto.getMontant();
        double nouveauReste = montantARembourser - nouveauMontantRembourse;

        emprunt.setMontantRembourse(nouveauMontantRembourse);
        emprunt.setResteAPayer(Math.max(0.0, nouveauReste));

        if (nouveauReste <= 0) {
            emprunt.setResteAPayer(0.0);
            emprunt.setStatutPaiement(StatutPaiement.PAYE);
        } else if (nouveauMontantRembourse > 0) {
            emprunt.setStatutPaiement(StatutPaiement.PARTIEL);
        } else {
            emprunt.setStatutPaiement(StatutPaiement.NON_PAYE);
        }

        empruntRepository.save(emprunt);

        RemboursementEmprunt saved = remboursementEmpruntRepository.save(remboursement);

        // ===== Opération comptable (dépense) créée pour CE remboursement =====
        operationComptableService.creerDepenseDepuisRemboursement(saved, emprunt.getEcole());

        return mapToDto(saved);
    }

    public List<RemboursementEmpruntResponseDTO> getByEmprunt(Long empruntId) {
        return remboursementEmpruntRepository
                .findByEmprunt_IdOrderByDateRemboursementDesc(empruntId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<RemboursementEmpruntResponseDTO> getByEcole(Long ecoleId) {
        return remboursementEmpruntRepository
                .findByEmprunt_Ecole_IdOrderByDateRemboursementDesc(ecoleId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private String genererReferenceCash() {
        return "EMP-" + System.currentTimeMillis();
    }

    private RemboursementEmpruntResponseDTO mapToDto(RemboursementEmprunt r) {

        Emprunt emprunt = r.getEmprunt();

        RemboursementEmpruntResponseDTO dto = new RemboursementEmpruntResponseDTO();

        dto.setId(r.getId());
        dto.setEmpruntId(emprunt.getId());
        dto.setEmpruntLibelle(emprunt.getLibelle());

        dto.setMontant(r.getMontant());
        dto.setModePaiement(r.getModePaiement());
        dto.setReference(r.getReference());
        dto.setDateRemboursement(r.getDateRemboursement());

        dto.setMontantTotal(emprunt.getMontantARembourser());
        dto.setMontantRembourseTotal(emprunt.getMontantRembourse());
        dto.setResteAPayer(emprunt.getResteAPayer());
        dto.setStatutPaiement(emprunt.getStatutPaiement() != null ? emprunt.getStatutPaiement().name() : null);

        return dto;
    }
}