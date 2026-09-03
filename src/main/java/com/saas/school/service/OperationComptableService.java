package com.saas.school.service;

import com.saas.school.dto.OperationComptableDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.OperationComptableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationComptableService {

    private final OperationComptableRepository operationComptableRepository;

    // =========================================================
    // CRÉER UNE RECETTE À PARTIR D'UN PAIEMENT DE SCOLARITÉ
    // =========================================================

    @Transactional
    public OperationComptable creerRecetteScolarite(PaiementScolarite paiement, Ecole ecole) {

        if (paiement == null) {
            throw new IllegalArgumentException("Le paiement est obligatoire");
        }

        if (ecole == null) {
            throw new IllegalArgumentException("L'école est obligatoire");
        }

        if (paiement.getMontant() == null || paiement.getMontant() <= 0) {
            throw new IllegalArgumentException("Le montant du paiement doit être supérieur à zéro");
        }

        if (operationComptableRepository.existsByPaiementScolarite_Id(paiement.getId())) {
            throw new IllegalStateException("Une opération comptable existe déjà pour ce paiement");
        }

        OperationComptable operation = new OperationComptable();

        operation.setEcole(ecole);
        operation.setNature(NatureOperation.RECETTE);
        operation.setMontant(paiement.getMontant());
        operation.setDateOperation(
                paiement.getDatePaiement() != null ? paiement.getDatePaiement() : LocalDateTime.now()
        );
        operation.setReference(paiement.getReference());
        operation.setModePaiement(paiement.getModePaiement());
        operation.setPaiementScolarite(paiement);

        String libelle = "Paiement scolarité";

        if (paiement.getLigneFrais() != null
                && paiement.getLigneFrais().getTypeFrais() != null
                && paiement.getLigneFrais().getTypeFrais().getLibelle() != null) {

            libelle = "Paiement " + paiement.getLigneFrais().getTypeFrais().getLibelle();
        }

        operation.setLibelle(libelle);

        return operationComptableRepository.save(operation);
    }

    // =========================================================
    // CRÉER UNE DÉPENSE (OPÉRATION) À PARTIR D'UN VERSEMENT
    // SUR UNE DÉPENSE (paiement en plusieurs tranches)
    // =========================================================

    @Transactional
    public OperationComptable creerDepenseDepuisPaiement(PaiementDepense paiementDepense, Ecole ecole) {

        if (paiementDepense == null) {
            throw new IllegalArgumentException("Le paiement de dépense est obligatoire");
        }

        if (ecole == null) {
            throw new IllegalArgumentException("L'école est obligatoire");
        }

        if (paiementDepense.getMontant() == null || paiementDepense.getMontant() <= 0) {
            throw new IllegalArgumentException("Le montant du versement doit être supérieur à zéro");
        }

        if (operationComptableRepository.existsByPaiementDepense_Id(paiementDepense.getId())) {
            throw new IllegalStateException("Une opération comptable existe déjà pour ce versement");
        }

        OperationComptable operation = new OperationComptable();

        operation.setEcole(ecole);
        operation.setNature(NatureOperation.DEPENSE);
        operation.setMontant(paiementDepense.getMontant());
        operation.setDateOperation(
                paiementDepense.getDatePaiement() != null
                        ? paiementDepense.getDatePaiement()
                        : LocalDateTime.now()
        );
        operation.setReference(paiementDepense.getReference());
        operation.setModePaiement(paiementDepense.getModePaiement());
        operation.setPaiementDepense(paiementDepense);

        String libelle = "Dépense";

        if (paiementDepense.getDepense() != null && paiementDepense.getDepense().getLibelle() != null) {
            libelle = paiementDepense.getDepense().getLibelle();
        }

        if (paiementDepense.getDepense() != null && paiementDepense.getDepense().getCategorie() != null) {
            operation.setCategorieDepense(paiementDepense.getDepense().getCategorie());
        }

        operation.setLibelle(libelle);

        return operationComptableRepository.save(operation);
    }

    // =========================================================
    // CRÉER UNE RECETTE LIBRE (don, subvention, location, etc. —
    // non liée à un paiement d'élève)
    // =========================================================

    @Transactional
    public OperationComptable creerRecette(
            Ecole ecole,
            Double montant,
            String libelle,
            String reference,
            String modePaiement
    ) {

        if (ecole == null) {
            throw new IllegalArgumentException("L'école est obligatoire");
        }

        if (montant == null || montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à zéro");
        }

        if (libelle == null || libelle.isBlank()) {
            throw new IllegalArgumentException("Le libellé de la recette est obligatoire");
        }

        OperationComptable operation = new OperationComptable();

        operation.setEcole(ecole);
        operation.setNature(NatureOperation.RECETTE);
        operation.setMontant(montant);
        operation.setDateOperation(LocalDateTime.now());
        operation.setLibelle(libelle);
        operation.setReference(reference);
        operation.setModePaiement(modePaiement);

        return operationComptableRepository.save(operation);
    }

    // =========================================================
    // CRÉER UNE DÉPENSE LIBRE (sans suivi d'échéancier)
    // =========================================================

    @Transactional
    public OperationComptable creerDepense(
            Ecole ecole,
            Double montant,
            String libelle,
            String reference,
            String modePaiement,
            CategorieDepense categorieDepense
    ) {

        if (ecole == null) {
            throw new IllegalArgumentException("L'école est obligatoire");
        }

        if (montant == null || montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être supérieur à zéro");
        }

        if (libelle == null || libelle.isBlank()) {
            throw new IllegalArgumentException("Le libellé de la dépense est obligatoire");
        }

        OperationComptable operation = new OperationComptable();

        operation.setEcole(ecole);
        operation.setNature(NatureOperation.DEPENSE);
        operation.setMontant(montant);
        operation.setDateOperation(LocalDateTime.now());
        operation.setLibelle(libelle);
        operation.setReference(reference);
        operation.setModePaiement(modePaiement);
        operation.setCategorieDepense(categorieDepense);

        return operationComptableRepository.save(operation);
    }
    // =========================================================
// CRÉER UNE OPÉRATION COMPTABLE À PARTIR D'UN REMBOURSEMENT
// D'EMPRUNT
// =========================================================

    @Transactional
    public OperationComptable creerDepenseDepuisRemboursement(
            RemboursementEmprunt remboursement,
            Ecole ecole
    ) {

        if (remboursement == null) {
            throw new IllegalArgumentException(
                    "Le remboursement d'emprunt est obligatoire"
            );
        }

        if (ecole == null) {
            throw new IllegalArgumentException(
                    "L'école est obligatoire"
            );
        }

        if (remboursement.getMontant() == null
                || remboursement.getMontant() <= 0) {

            throw new IllegalArgumentException(
                    "Le montant du remboursement doit être supérieur à zéro"
            );
        }

        if (remboursement.getId() == null) {
            throw new IllegalArgumentException(
                    "Le remboursement doit être enregistré avant de créer l'opération comptable"
            );
        }

        // Évite de créer deux opérations pour le même remboursement
        if (operationComptableRepository
                .existsByRemboursementEmprunt_Id(remboursement.getId())) {

            throw new IllegalStateException(
                    "Une opération comptable existe déjà pour ce remboursement"
            );
        }

        OperationComptable operation = new OperationComptable();

        operation.setEcole(ecole);

        // IMPORTANT :
        // Un remboursement d'emprunt n'est pas une dépense ordinaire.
        operation.setNature(NatureOperation.REMBOURSEMENT_EMPRUNT);

        operation.setMontant(remboursement.getMontant());

        operation.setDateOperation(
                remboursement.getDateRemboursement() != null
                        ? remboursement.getDateRemboursement()
                        : LocalDateTime.now()
        );

        operation.setReference(remboursement.getReference());

        operation.setModePaiement(
                remboursement.getModePaiement()
        );

        operation.setRemboursementEmprunt(remboursement);

        // Libellé
        String libelle = "Remboursement d'emprunt";

        if (remboursement.getEmprunt() != null
                && remboursement.getEmprunt().getLibelle() != null
                && !remboursement.getEmprunt().getLibelle().isBlank()) {

            libelle = "Remboursement - "
                    + remboursement.getEmprunt().getLibelle();
        }

        operation.setLibelle(libelle);

        return operationComptableRepository.save(operation);
    }
    // =========================================================
// CRÉER UNE OPÉRATION COMPTABLE À PARTIR D'UN EMPRUNT
// =========================================================

    @Transactional
    public OperationComptable creerRecetteEmprunt(
            Emprunt emprunt,
            Ecole ecole
    ) {

        if (emprunt == null) {
            throw new IllegalArgumentException(
                    "L'emprunt est obligatoire"
            );
        }

        if (ecole == null) {
            throw new IllegalArgumentException(
                    "L'école est obligatoire"
            );
        }

        if (emprunt.getId() == null) {
            throw new IllegalArgumentException(
                    "L'emprunt doit être enregistré avant de créer l'opération comptable"
            );
        }

        if (emprunt.getMontantEmprunte() == null
                || emprunt.getMontantEmprunte() <= 0) {

            throw new IllegalArgumentException(
                    "Le montant emprunté doit être supérieur à zéro"
            );
        }

        if (operationComptableRepository
                .existsByEmprunt_Id(emprunt.getId())) {

            throw new IllegalStateException(
                    "Une opération comptable existe déjà pour cet emprunt"
            );
        }

        OperationComptable operation = new OperationComptable();

        operation.setEcole(ecole);

        // Un emprunt augmente la trésorerie,
        // mais ce n'est pas une recette.
        operation.setNature(NatureOperation.EMPRUNT);

        // IMPORTANT :
        // seul l'argent réellement reçu entre en trésorerie.
        operation.setMontant(emprunt.getMontantEmprunte());

        operation.setDateOperation(
                emprunt.getDateEmprunt() != null
                        ? emprunt.getDateEmprunt()
                        : LocalDateTime.now()
        );

        operation.setLibelle(
                "Emprunt - " + emprunt.getLibelle()
        );

        operation.setReference(
                "EMP-" + emprunt.getId()
        );

        operation.setEmprunt(emprunt);

        return operationComptableRepository.save(operation);
    }

    @Transactional
    public OperationComptable creerDepenseDepuisPaiementEnseignant(
            PaiementEnseignant paiement) {

        if (paiement == null) {
            throw new IllegalArgumentException("Le paiement enseignant est obligatoire");
        }

        if (paiement.getEnseignant() == null) {
            throw new IllegalArgumentException("L'enseignant est obligatoire");
        }

        if (paiement.getMontant() == null || paiement.getMontant() <= 0) {
            throw new IllegalArgumentException(
                    "Le montant du paiement enseignant doit être supérieur à zéro"
            );
        }

        Ecole ecole = paiement.getEnseignant().getEcole();

        if (ecole == null) {
            throw new IllegalArgumentException(
                    "L'enseignant n'est associé à aucune école"
            );
        }

        OperationComptable operation = new OperationComptable();

        operation.setEcole(ecole);
        operation.setNature(NatureOperation.DEPENSE);
        operation.setMontant(paiement.getMontant());

        operation.setDateOperation(
                paiement.getDatePaiement() != null
                        ? paiement.getDatePaiement().atStartOfDay()
                        : LocalDateTime.now()
        );

        operation.setLibelle(
                "Paiement enseignant - "
                        + paiement.getEnseignant().getNom()
                        + " "
                        + paiement.getEnseignant().getPrenom()
        );



        return operationComptableRepository.save(operation);
    }
    // =========================================================
    // RAPPORT COMPTABLE GLOBAL
    // =========================================================

    @Transactional(readOnly = true)
    public OperationComptableDTO genererRapport(Long ecoleId) {

        if (ecoleId == null) {
            throw new IllegalArgumentException("L'école est obligatoire");
        }

        List<OperationComptable> operations =
                operationComptableRepository
                        .findByEcole_IdOrderByDateOperationDesc(ecoleId);

        List<OperationComptableDTO> operationsDTO =
                operations.stream()
                        .map(this::mapToDTO)
                        .toList();

        double totalRecettes = 0.0;
        double totalDepenses = 0.0;
        double totalEmprunts = 0.0;
        double totalRemboursements = 0.0;

        for (OperationComptable operation : operations) {

            if (operation.getMontant() == null) {
                continue;
            }

            switch (operation.getNature()) {

                case RECETTE ->
                        totalRecettes += operation.getMontant();

                case DEPENSE ->
                        totalDepenses += operation.getMontant();

                case EMPRUNT ->
                        totalEmprunts += operation.getMontant();

                case REMBOURSEMENT_EMPRUNT ->
                        totalRemboursements += operation.getMontant();
            }
        }

        double tresorerie =
                totalRecettes
                        + totalEmprunts
                        - totalDepenses
                        - totalRemboursements;

        OperationComptableDTO rapport = new OperationComptableDTO();

        rapport.setTotalRecettes(totalRecettes);
        rapport.setTotalDepenses(totalDepenses);
        rapport.setTotalEmprunts(totalEmprunts);
        rapport.setTotalRemboursements(totalRemboursements);
        rapport.setSolde(tresorerie);
        rapport.setNombreOperations(operations.size());
        rapport.setOperations(operationsDTO);

        return rapport;
    }
    // =========================================================
    // CONVERSION PUBLIQUE (utilisée par le contrôleur après création)
    // =========================================================

    public OperationComptableDTO toDto(OperationComptable operation) {
        return mapToDTO(operation);
    }

    // =========================================================
    // CONVERSION OPERATION → DTO
    // =========================================================

    private OperationComptableDTO mapToDTO(OperationComptable operation) {

        OperationComptableDTO dto = new OperationComptableDTO();

        dto.setId(operation.getId());

        // ===== ÉCOLE =====
        if (operation.getEcole() != null) {
            dto.setEcoleId(operation.getEcole().getId());
        }

        // ===== INFORMATIONS GÉNÉRALES =====
        dto.setLibelle(operation.getLibelle());
        dto.setMontant(operation.getMontant());
        dto.setDateOperation(operation.getDateOperation());
        dto.setReference(operation.getReference());
        dto.setModePaiement(operation.getModePaiement());

        if (operation.getNature() != null) {
            dto.setNature(operation.getNature().name());
        }

        // ===== DÉPENSE =====
        if (operation.getCategorieDepense() != null) {
            dto.setCategorieDepenseId(operation.getCategorieDepense().getId());
            dto.setCategorieDepenseNom(operation.getCategorieDepense().getNom());
        }

        // ===== PAIEMENT SCOLARITÉ =====
        if (operation.getPaiementScolarite() != null) {

            dto.setTypeOperation("PAIEMENT_SCOLARITE");

            Long paiementId = operation.getPaiementScolarite().getId();

            dto.setPaiementScolariteId(paiementId);
            dto.setReferenceId(paiementId);

            var paiement = operation.getPaiementScolarite();

            if (paiement.getLigneFrais() != null) {

                var ligne = paiement.getLigneFrais();

                if (ligne.getTypeFrais() != null) {
                    dto.setTypeFraisNom(ligne.getTypeFrais().getLibelle());
                }

                if (ligne.getInscription() != null) {

                    var inscription = ligne.getInscription();

                    dto.setInscriptionId(inscription.getId());

                    if (inscription.getEleve() != null) {
                        dto.setEleveNom(inscription.getEleve().getNom());
                        dto.setElevePrenom(inscription.getEleve().getPrenom());
                    }
                }
            }

        } else if (operation.getPaiementDepense() != null) {

            dto.setTypeOperation("DEPENSE_VERSEMENT");

            Long paiementDepenseId = operation.getPaiementDepense().getId();

            dto.setReferenceId(paiementDepenseId);

            if (operation.getPaiementDepense().getDepense() != null) {
                dto.setDepenseId(operation.getPaiementDepense().getDepense().getId());
            }

        } else if (operation.getNature() == NatureOperation.RECETTE) {

            dto.setTypeOperation("RECETTE_LIBRE");


        } else {
            dto.setTypeOperation("DEPENSE");
        }

        return dto;
    }
}