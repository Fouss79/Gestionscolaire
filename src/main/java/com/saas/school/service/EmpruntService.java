package com.saas.school.service;

import com.saas.school.dto.EmpruntDTO;
import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Emprunt;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.EmpruntRepository;
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
    private final AnneeScolaireRepository anneeScolaireRepository;


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

        if (ecoleId == null) {
            throw new RuntimeException(
                    "L'école est obligatoire"
            );
        }

        // =========================
        // VALIDATION
        // =========================

        if (dto.getLibelle() == null
                || dto.getLibelle().isBlank()) {

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
        // ANNÉE SCOLAIRE
        // =========================

        if (dto.getAnneeScolaireId() == null) {
            throw new RuntimeException(
                    "L'année scolaire est obligatoire"
            );
        }

        // =========================
        // ÉCOLE
        // =========================

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "École introuvable"
                        )
                );

        // =========================
        // ANNÉE
        // =========================

        AnneeScolaire anneeScolaire =
                anneeScolaireRepository
                        .findById(dto.getAnneeScolaireId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Année scolaire introuvable"
                                )
                        );

        // =========================
        // VÉRIFIER L'ÉCOLE
        // =========================

        if (anneeScolaire.getEcole() == null) {
            throw new RuntimeException(
                    "L'année scolaire n'est associée à aucune école"
            );
        }

        if (!anneeScolaire.getEcole().getId()
                .equals(ecole.getId())) {

            throw new RuntimeException(
                    "Cette année scolaire n'appartient pas à cette école"
            );
        }

        // =========================
        // ENTITY
        // =========================

        Emprunt emprunt = new Emprunt();

        emprunt.setEcole(ecole);

        // IMPORTANT :
        // l'année scolaire est enregistrée directement
        // sur l'emprunt.
        emprunt.setAnneeScolaire(anneeScolaire);

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

        Emprunt saved =
                empruntRepository.save(emprunt);

        // =========================
        // OPÉRATION COMPTABLE
        // =========================
        //
        // L'argent reçu augmente la trésorerie.
        // Mais l'emprunt n'est PAS une recette.
        //
        // Nature = EMPRUNT
        //
        // L'opération récupérera l'année directement
        // depuis saved.getAnneeScolaire().

        operationComptableService.creerRecetteEmprunt(
                saved,
                ecole
        );

        return toDTO(saved);
    }


    // =========================================================
    // RÉCUPÉRER LES EMPRUNTS D'UNE ÉCOLE POUR UNE ANNÉE
    // =========================================================

    public List<EmpruntDTO> getByEcole(
            Long ecoleId,
            Long anneeId
    ) {

        if (ecoleId == null) {
            throw new IllegalArgumentException(
                    "L'école est obligatoire"
            );
        }

        if (anneeId == null) {
            throw new IllegalArgumentException(
                    "L'année scolaire est obligatoire"
            );
        }

        // =========================
        // VÉRIFIER L'ANNÉE
        // =========================

        AnneeScolaire anneeScolaire =
                anneeScolaireRepository
                        .findById(anneeId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Année scolaire introuvable"
                                )
                        );

        // =========================
        // VÉRIFIER L'ÉCOLE
        // =========================

        if (anneeScolaire.getEcole() == null
                || !anneeScolaire.getEcole()
                .getId()
                .equals(ecoleId)) {

            throw new IllegalArgumentException(
                    "Cette année scolaire n'appartient pas à cette école"
            );
        }

        // =====================================================
        // IMPORTANT :
        // ON NE FILTRE PLUS PAR DATE
        // =====================================================

        return empruntRepository
                .findByEcole_IdAndAnneeScolaire_IdOrderByDateEmpruntDesc(
                        ecoleId,
                        anneeId
                )
                .stream()
                .map(this::toDTO)
                .toList();
    }


    // =========================================================
    // ALIAS
    // =========================================================

    public List<EmpruntDTO> findByEcole(
            Long ecoleId,
            Long anneeId
    ) {
        return getByEcole(ecoleId, anneeId);
    }


    // =========================================================
    // RÉCUPÉRER UN EMPRUNT
    // =========================================================

    public EmpruntDTO getById(Long id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "L'identifiant de l'emprunt est obligatoire"
            );
        }

        Emprunt emprunt =
                empruntRepository.findById(id)
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

    public EmpruntDTO toDTO(
            Emprunt emprunt
    ) {

        if (emprunt == null) {
            return null;
        }

        EmpruntDTO dto =
                new EmpruntDTO();

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
        // ANNÉE SCOLAIRE
        // =========================

        if (emprunt.getAnneeScolaire() != null) {

            dto.setAnneeScolaireId(
                    emprunt.getAnneeScolaire().getId()
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