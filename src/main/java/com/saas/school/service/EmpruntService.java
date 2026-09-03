package com.saas.school.service;

import com.saas.school.dto.EmpruntDTO;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Emprunt;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.EmpruntRepository;
import com.saas.school.service.StatutPaiement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpruntService {

    private final EmpruntRepository empruntRepository;
    private final EcoleRepository ecoleRepository;
    private final OperationComptableService operationComptableService;

    // =========================================================
    // CRÉER UN EMPRUNT
    // =========================================================

    @Transactional
    public EmpruntDTO creer(Long ecoleId, EmpruntDTO dto) {

        if (dto == null) {
            throw new RuntimeException(
                    "Les données de l'emprunt sont obligatoires"
            );
        }

        // =========================
        // VALIDATION
        // =========================

        if (dto.getLibelle() == null || dto.getLibelle().isBlank()) {
            throw new RuntimeException(
                    "Le libellé est obligatoire"
            );
        }

        if (dto.getMontantEmprunte() == null
                || dto.getMontantEmprunte() <= 0) {

            throw new RuntimeException(
                    "Le montant emprunté doit être supérieur à zéro"
            );
        }

        if (dto.getMontantARembourser() == null
                || dto.getMontantARembourser() <= 0) {

            throw new RuntimeException(
                    "Le montant à rembourser doit être supérieur à zéro"
            );
        }

        if (dto.getMontantARembourser()
                < dto.getMontantEmprunte()) {

            throw new RuntimeException(
                    "Le montant à rembourser ne peut pas être inférieur au montant emprunté"
            );
        }

        // =========================
        // ÉCOLE
        // =========================

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() ->
                        new RuntimeException("École introuvable")
                );

        // =========================
        // ENTITY
        // =========================

        Emprunt emprunt = new Emprunt();

        emprunt.setEcole(ecole);

        emprunt.setLibelle(
                dto.getLibelle().trim()
        );

        emprunt.setMontantEmprunte(
                dto.getMontantEmprunte()
        );

        emprunt.setMontantARembourser(
                dto.getMontantARembourser()
        );

        emprunt.setMontantRembourse(0.0);

        emprunt.setResteAPayer(
                dto.getMontantARembourser()
        );

        emprunt.setDateEmprunt(
                dto.getDateEmprunt() != null
                        ? dto.getDateEmprunt()
                        : LocalDateTime.now()
        );

        emprunt.setDateEcheance(
                dto.getDateEcheance()
        );

        emprunt.setStatutPaiement(
                StatutPaiement.NON_PAYE
        );

        // =========================
        // SAUVEGARDE
        // =========================

        Emprunt saved = empruntRepository.save(emprunt);

        // =========================
        // OPÉRATION COMPTABLE
        // =========================
        //
        // L'argent reçu augmente la trésorerie.
        // Mais l'emprunt n'est PAS une recette.
        //
        // Nature = EMPRUNT

        operationComptableService.creerRecetteEmprunt(
                saved,
                ecole
        );

        return toDTO(saved);
    }

    // =========================================================
    // RÉCUPÉRER TOUS LES EMPRUNTS D'UNE ÉCOLE
    // =========================================================

    public List<EmpruntDTO> getByEcole(Long ecoleId) {

        return empruntRepository
                .findByEcole_IdOrderByDateEmpruntDesc(ecoleId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // =========================================================
    // ALIAS
    // =========================================================

    public List<EmpruntDTO> findByEcole(Long ecoleId) {
        return getByEcole(ecoleId);
    }

    // =========================================================
    // RÉCUPÉRER UN EMPRUNT
    // =========================================================

    public EmpruntDTO getById(Long id) {

        Emprunt emprunt = empruntRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Emprunt introuvable"
                        )
                );

        return toDTO(emprunt);
    }

    // =========================================================
    // CONVERSION ENTITY -> DTO
    // =========================================================

    public EmpruntDTO toDTO(Emprunt emprunt) {

        if (emprunt == null) {
            return null;
        }

        EmpruntDTO dto = new EmpruntDTO();

        // =========================
        // ID
        // =========================

        dto.setId(
                emprunt.getId()
        );

        // =========================
        // ÉCOLE
        // =========================

        if (emprunt.getEcole() != null) {
            dto.setEcoleId(
                    emprunt.getEcole().getId()
            );
        }

        // =========================
        // INFORMATIONS
        // =========================

        dto.setLibelle(
                emprunt.getLibelle()
        );

        // =========================
        // MONTANTS
        // =========================

        dto.setMontantEmprunte(
                emprunt.getMontantEmprunte()
        );

        dto.setMontantARembourser(
                emprunt.getMontantARembourser()
        );

        dto.setMontantRembourse(
                emprunt.getMontantRembourse()
        );

        dto.setResteAPayer(
                emprunt.getResteAPayer()
        );

        // =========================
        // DATES
        // =========================

        dto.setDateEmprunt(
                emprunt.getDateEmprunt()
        );

        dto.setDateEcheance(
                emprunt.getDateEcheance()
        );

        // =========================
        // STATUT
        // =========================

        dto.setStatutPaiement(
                emprunt.getStatutPaiement() != null
                        ? emprunt.getStatutPaiement().name()
                        : null
        );

        return dto;
    }
}
