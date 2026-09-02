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
    // CRÉER UNE DÉPENSE
    // =========================================================

    // =========================================================
// CRÉER UNE OPÉRATION COMPTABLE POUR UNE DÉPENSE
// =========================================================

    @Transactional
    public OperationComptable creerDepense(Depense depense) {

        if (depense == null) {
            throw new IllegalArgumentException("La dépense est obligatoire");
        }

        if (depense.getEcole() == null) {
            throw new IllegalArgumentException("L'école est obligatoire");
        }

        if (depense.getMontant() == null || depense.getMontant() <= 0) {
            throw new IllegalArgumentException(
                    "Le montant de la dépense doit être supérieur à zéro"
            );
        }

        if (depense.getLibelle() == null || depense.getLibelle().isBlank()) {
            throw new IllegalArgumentException(
                    "Le libellé de la dépense est obligatoire"
            );
        }

        OperationComptable operation = new OperationComptable();

        operation.setEcole(depense.getEcole());

        operation.setNature(NatureOperation.DEPENSE);

        operation.setMontant(depense.getMontant());

        // On utilise la vraie date de la dépense
        if (depense.getDateDepense() != null) {
            operation.setDateOperation(
                    depense.getDateDepense().atStartOfDay()
            );
        } else {
            operation.setDateOperation(LocalDateTime.now());
        }

        operation.setLibelle(depense.getLibelle());



        operation.setCategorieDepense(
                depense.getCategorie()
        );
        System.out.println("=================================");
        System.out.println("Categorie opération ID = " +
                (operation.getCategorieDepense() != null
                        ? operation.getCategorieDepense().getId()
                        : null));
        System.out.println("Categorie opération nom = " +
                (operation.getCategorieDepense() != null
                        ? operation.getCategorieDepense().getNom()
                        : null));
        System.out.println("=================================");
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
                operationComptableRepository.findByEcole_IdOrderByDateOperationDesc(ecoleId);

        double totalRecettes = 0.0;
        double totalDepenses = 0.0;

        List<OperationComptableDTO> operationsDTO = operations.stream()
                .map(this::mapToDTO)
                .toList();

        for (OperationComptable operation : operations) {

            if (operation.getMontant() == null) {
                continue;
            }

            if (operation.getNature() == NatureOperation.RECETTE) {
                totalRecettes += operation.getMontant();
            } else if (operation.getNature() == NatureOperation.DEPENSE) {
                totalDepenses += operation.getMontant();
            }
        }

        double solde = totalRecettes - totalDepenses;

        OperationComptableDTO rapport = new OperationComptableDTO();

        rapport.setTotalRecettes(totalRecettes);
        rapport.setTotalDepenses(totalDepenses);
        rapport.setSolde(solde);
        rapport.setNombreOperations(operations.size());
        rapport.setOperations(operationsDTO);

        return rapport;
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

        } else {
            dto.setTypeOperation("DEPENSE");
        }

        return dto;
    }
}