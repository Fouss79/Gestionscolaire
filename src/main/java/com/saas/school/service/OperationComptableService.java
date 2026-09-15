package com.saas.school.service;

import com.saas.school.dto.OperationComptableDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.OperationComptableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OperationComptableService {

    private final OperationComptableRepository operationComptableRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;

    // ============================================================
    // RECETTE SCOLARITÉ
    // ============================================================

    public OperationComptable creerRecetteScolarite(
            PaiementScolarite paiement,
            Ecole ecole
    ) {

        if (paiement == null) {
            throw new RuntimeException("Le paiement de scolarité est obligatoire.");
        }

        if (ecole == null || ecole.getId() == null) {
            throw new RuntimeException("L'école est obligatoire.");
        }

        if (paiement.getMontant() == null || paiement.getMontant() <= 0) {
            throw new RuntimeException("Le montant du paiement doit être supérieur à zéro.");
        }

        if (paiement.getLigneFrais() == null) {
            throw new RuntimeException(
                    "La ligne de frais associée au paiement est obligatoire."
            );
        }

        if (operationComptableRepository
                .existsByPaiementScolarite_Id(paiement.getId())) {

            throw new RuntimeException(
                    "Une opération comptable existe déjà pour ce paiement de scolarité."
            );
        }

        Inscription inscription =
                paiement.getLigneFrais().getInscription();

        if (inscription == null) {
            throw new RuntimeException(
                    "L'inscription associée au paiement est introuvable."
            );
        }

        AnneeScolaire anneeScolaire =
                inscription.getAnneeScolaire();

        verifierAnneeAppartientEcole(
                anneeScolaire,
                ecole
        );

        OperationComptable operation = new OperationComptable();

        operation.setEcole(ecole);
        operation.setAnneeScolaire(anneeScolaire);

        operation.setNature(NatureOperation.RECETTE);
        operation.setMontant(paiement.getMontant());

        operation.setDateOperation(
                paiement.getDatePaiement() != null
                        ? paiement.getDatePaiement()
                        : LocalDateTime.now()
        );

        operation.setLibelle("Paiement scolarité");
        operation.setReference(paiement.getReference());
        operation.setModePaiement(paiement.getModePaiement());

        operation.setPaiementScolarite(paiement);

        return operationComptableRepository.save(operation);
    }

    // ============================================================
    // PAIEMENT D'UNE DÉPENSE
    // ============================================================

    public OperationComptable creerDepenseDepuisPaiement(
            PaiementDepense paiementDepense,
            Ecole ecole
    ) {

        if (paiementDepense == null) {
            throw new RuntimeException("Le paiement de dépense est obligatoire.");
        }

        if (ecole == null || ecole.getId() == null) {
            throw new RuntimeException("L'école est obligatoire.");
        }

        if (paiementDepense.getMontant() == null
                || paiementDepense.getMontant() <= 0) {

            throw new RuntimeException(
                    "Le montant du paiement doit être supérieur à zéro."
            );
        }

        if (paiementDepense.getDepense() == null) {
            throw new RuntimeException(
                    "La dépense associée au paiement est obligatoire."
            );
        }

        if (operationComptableRepository
                .existsByPaiementDepense_Id(paiementDepense.getId())) {

            throw new RuntimeException(
                    "Une opération comptable existe déjà pour ce paiement de dépense."
            );
        }

        Depense depense = paiementDepense.getDepense();

        AnneeScolaire anneeScolaire =
                depense.getAnneeScolaire();

        verifierAnneeAppartientEcole(
                anneeScolaire,
                ecole
        );

        OperationComptable operation = new OperationComptable();

        operation.setEcole(ecole);
        operation.setAnneeScolaire(anneeScolaire);

        /*
         * Important :
         * on enregistre uniquement le montant réellement payé,
         * et non le montantTotal de la dépense.
         */
        operation.setNature(NatureOperation.DEPENSE);
        operation.setMontant(paiementDepense.getMontant());

        operation.setDateOperation(
                paiementDepense.getDatePaiement() != null
                        ? paiementDepense.getDatePaiement()
                        : LocalDateTime.now()
        );

        operation.setLibelle(
                depense.getLibelle() != null
                        ? depense.getLibelle()
                        : "Paiement dépense"
        );

        operation.setReference(
                paiementDepense.getReference()
        );

        operation.setModePaiement(
                paiementDepense.getModePaiement()
        );

        operation.setPaiementDepense(paiementDepense);

        if (depense.getCategorie() != null) {
            operation.setCategorieDepense(
                    depense.getCategorie()
            );
        }

        return operationComptableRepository.save(operation);
    }

    // ============================================================
    // RECETTE LIBRE
    // ============================================================

    public OperationComptable creerRecette(
            Ecole ecole,
            Double montant,
            String libelle,
            String reference,
            String modePaiement,
            LocalDate dateRecette,
            Long anneeScolaireId
    ) {

        verifierEcole(ecole);
        verifierMontant(montant);

        if (anneeScolaireId == null) {
            throw new RuntimeException(
                    "L'année scolaire est obligatoire."
            );
        }

        if (dateRecette == null) {
            throw new RuntimeException(
                    "La date de la recette est obligatoire."
            );
        }

        AnneeScolaire anneeScolaire =
                trouverAnneeScolaire(anneeScolaireId);

        verifierAnneeAppartientEcole(
                anneeScolaire,
                ecole
        );

        verifierDateDansAnnee(
                dateRecette,
                anneeScolaire
        );

        OperationComptable operation = new OperationComptable();

        operation.setEcole(ecole);
        operation.setAnneeScolaire(anneeScolaire);

        operation.setNature(NatureOperation.RECETTE);
        operation.setMontant(montant);

        /*
         * On respecte la date choisie par l'administration.
         */
        operation.setDateOperation(
                dateRecette.atStartOfDay()
        );

        operation.setLibelle(
                libelle != null && !libelle.trim().isEmpty()
                        ? libelle.trim()
                        : "Recette"
        );

        operation.setReference(reference);
        operation.setModePaiement(modePaiement);

        return operationComptableRepository.save(operation);
    }

    // ============================================================
    // DÉPENSE LIBRE
    // ============================================================

    public OperationComptable creerDepense(
            Ecole ecole,
            Double montant,
            String libelle,
            String reference,
            String modePaiement,
            CategorieDepense categorieDepense,
            Long anneeScolaireId
    ) {

        verifierEcole(ecole);
        verifierMontant(montant);

        if (anneeScolaireId == null) {
            throw new RuntimeException(
                    "L'année scolaire est obligatoire."
            );
        }

        AnneeScolaire anneeScolaire =
                trouverAnneeScolaire(anneeScolaireId);

        verifierAnneeAppartientEcole(
                anneeScolaire,
                ecole
        );

        if (categorieDepense != null
                && categorieDepense.getEcole() != null
                && categorieDepense.getEcole().getId() != null
                && !categorieDepense.getEcole().getId().equals(ecole.getId())) {

            throw new RuntimeException(
                    "La catégorie de dépense n'appartient pas à cette école."
            );
        }

        OperationComptable operation = new OperationComptable();

        operation.setEcole(ecole);
        operation.setAnneeScolaire(anneeScolaire);

        operation.setNature(NatureOperation.DEPENSE);
        operation.setMontant(montant);

        operation.setDateOperation(
                LocalDateTime.now()
        );

        operation.setLibelle(
                libelle != null && !libelle.trim().isEmpty()
                        ? libelle.trim()
                        : "Dépense"
        );

        operation.setReference(reference);
        operation.setModePaiement(modePaiement);
        operation.setCategorieDepense(categorieDepense);

        return operationComptableRepository.save(operation);
    }

    // ============================================================
    // REMBOURSEMENT D'EMPRUNT
    // ============================================================

    public OperationComptable creerDepenseDepuisRemboursement(
            RemboursementEmprunt remboursement,
            Ecole ecole
    ) {

        if (remboursement == null) {
            throw new RuntimeException(
                    "Le remboursement est obligatoire."
            );
        }

        verifierEcole(ecole);

        if (remboursement.getMontant() == null
                || remboursement.getMontant() <= 0) {

            throw new RuntimeException(
                    "Le montant du remboursement doit être supérieur à zéro."
            );
        }

        if (operationComptableRepository
                .existsByRemboursementEmprunt_Id(
                        remboursement.getId()
                )) {

            throw new RuntimeException(
                    "Une opération comptable existe déjà pour ce remboursement."
            );
        }

        if (remboursement.getEmprunt() == null) {
            throw new RuntimeException(
                    "L'emprunt associé au remboursement est obligatoire."
            );
        }

        Emprunt emprunt =
                remboursement.getEmprunt();

        AnneeScolaire anneeScolaire =
                remboursement.getAnneeScolaire();
        verifierAnneeAppartientEcole(
                anneeScolaire,
                ecole
        );

        OperationComptable operation = new OperationComptable();

        operation.setEcole(ecole);
        operation.setAnneeScolaire(anneeScolaire);

        /*
         * IMPORTANT :
         * un remboursement d'emprunt n'est pas une dépense ordinaire.
         *
         * Il doit apparaître séparément dans :
         * - Remboursements
         * - Trésorerie
         *
         * mais pas dans Total dépenses.
         */
        operation.setNature(
                NatureOperation.REMBOURSEMENT_EMPRUNT
        );

        operation.setMontant(
                remboursement.getMontant()
        );

        operation.setDateOperation(
                remboursement.getDateRemboursement() != null
                        ? remboursement.getDateRemboursement()
                        : LocalDateTime.now()
        );

        operation.setLibelle(
                "Remboursement emprunt - "
                        + (
                        emprunt.getLibelle() != null
                                ? emprunt.getLibelle()
                                : "Emprunt #" + emprunt.getId()
                )
        );

        operation.setReference(
                remboursement.getReference()
        );

        operation.setModePaiement(
                remboursement.getModePaiement()
        );

        operation.setRemboursementEmprunt(
                remboursement
        );

        return operationComptableRepository.save(operation);
    }

    // ============================================================
    // EMPRUNT
    // ============================================================

    public OperationComptable creerRecetteEmprunt(
            Emprunt emprunt,
            Ecole ecole
    ) {

        if (emprunt == null) {
            throw new RuntimeException(
                    "L'emprunt est obligatoire."
            );
        }

        verifierEcole(ecole);

        if (emprunt.getMontantEmprunte() == null
                || emprunt.getMontantEmprunte() <= 0) {

            throw new RuntimeException(
                    "Le montant de l'emprunt doit être supérieur à zéro."
            );
        }

        if (operationComptableRepository
                .existsByEmprunt_Id(emprunt.getId())) {

            throw new RuntimeException(
                    "Une opération comptable existe déjà pour cet emprunt."
            );
        }

        AnneeScolaire anneeScolaire =
                emprunt.getAnneeScolaire();

        verifierAnneeAppartientEcole(
                anneeScolaire,
                ecole
        );

        OperationComptable operation =
                new OperationComptable();

        operation.setEcole(ecole);
        operation.setAnneeScolaire(anneeScolaire);

        operation.setNature(
                NatureOperation.EMPRUNT
        );

        operation.setMontant(
                emprunt.getMontantEmprunte()
        );

        operation.setDateOperation(
                emprunt.getDateEmprunt() != null
                        ? emprunt.getDateEmprunt()
                        : LocalDateTime.now()
        );

        operation.setLibelle(
                emprunt.getLibelle() != null
                        ? emprunt.getLibelle()
                        : "Emprunt #" + emprunt.getId()
        );

        operation.setReference(
                "EMP-" + emprunt.getId()
        );

        operation.setEmprunt(emprunt);

        return operationComptableRepository.save(operation);
    }

    // ============================================================
    // PAIEMENT ENSEIGNANT
    // ============================================================

    public OperationComptable creerDepenseDepuisPaiementEnseignant(
            PaiementEnseignant paiement
    ) {

        if (paiement == null) {
            throw new RuntimeException(
                    "Le paiement enseignant est obligatoire."
            );
        }

        if (paiement.getId() == null) {
            throw new RuntimeException(
                    "Le paiement enseignant doit être enregistré avant la création de l'opération."
            );
        }

        if (paiement.getMontant() == null
                || paiement.getMontant() <= 0) {

            throw new RuntimeException(
                    "Le montant du paiement enseignant doit être supérieur à zéro."
            );
        }

        if (paiement.getEnseignant() == null) {
            throw new RuntimeException(
                    "L'enseignant associé au paiement est obligatoire."
            );
        }

        Ecole ecole =
                paiement.getEnseignant().getEcole();

        verifierEcole(ecole);

        if (operationComptableRepository
                .existsByPaiementEnseignantId(
                        paiement.getId()
                )) {

            throw new RuntimeException(
                    "Une opération comptable existe déjà pour ce paiement enseignant."
            );
        }

        if (paiement.getAnneeScolaireId() == null) {
            throw new RuntimeException(
                    "L'année scolaire du paiement enseignant est obligatoire."
            );
        }

        AnneeScolaire anneeScolaire =
                trouverAnneeScolaire(
                        paiement.getAnneeScolaireId()
                );

        verifierAnneeAppartientEcole(
                anneeScolaire,
                ecole
        );

        OperationComptable operation =
                new OperationComptable();

        operation.setEcole(ecole);
        operation.setAnneeScolaire(anneeScolaire);

        operation.setNature(
                NatureOperation.DEPENSE
        );

        operation.setMontant(
                paiement.getMontant()
        );

        /*
         * Si datePaiement est LocalDate :
         *     atStartOfDay()
         *
         * Si datePaiement est LocalDateTime :
         *     utiliser directement la valeur.
         */
        operation.setDateOperation(
                paiement.getDatePaiement() != null
                        ? paiement.getDatePaiement().atStartOfDay()
                        : LocalDateTime.now()
        );

        String nomEnseignant =
                (
                        paiement.getEnseignant().getNom() != null
                                ? paiement.getEnseignant().getNom()
                                : ""
                )
                        + " "
                        + (
                        paiement.getEnseignant().getPrenom() != null
                                ? paiement.getEnseignant().getPrenom()
                                : ""
                );

        operation.setLibelle(
                "Paiement enseignant - "
                        + nomEnseignant.trim()
        );

        operation.setReference(
                "ENS-" + paiement.getId()
        );

        operation.setPaiementEnseignantId(
                paiement.getId()
        );

        return operationComptableRepository.save(operation);
    }

    private OperationComptableDTO construireRapport(
            List<OperationComptableDTO> operations,
            Long anneeId
    ) {
        double totalRecettes = operations.stream()
                .filter(op -> "RECETTE".equals(op.getNature()))
                .mapToDouble(op -> op.getMontant() != null ? op.getMontant() : 0.0)
                .sum();

        double totalDepenses = operations.stream()
                .filter(op -> "DEPENSE".equals(op.getNature()))
                .mapToDouble(op -> op.getMontant() != null ? op.getMontant() : 0.0)
                .sum();

        double totalEmprunts = operations.stream()
                .filter(op -> "EMPRUNT".equals(op.getNature()))
                .mapToDouble(op -> op.getMontant() != null ? op.getMontant() : 0.0)
                .sum();

        double totalRemboursements = operations.stream()
                .filter(op -> "REMBOURSEMENT_EMPRUNT".equals(op.getNature()))
                .mapToDouble(op -> op.getMontant() != null ? op.getMontant() : 0.0)
                .sum();

        double solde = totalRecettes + totalEmprunts - totalDepenses - totalRemboursements;

        OperationComptableDTO rapport = new OperationComptableDTO();
        rapport.setAnneeScolaireId(anneeId);
        rapport.setTotalRecettes(totalRecettes);
        rapport.setTotalDepenses(totalDepenses);
        rapport.setTotalEmprunts(totalEmprunts);
        rapport.setTotalRemboursements(totalRemboursements);
        rapport.setSolde(solde);
        rapport.setNombreOperations(operations.size());
        rapport.setOperations(operations);

        return rapport;
    }


    // ============================================================
    // RAPPORT PAR ANNÉE SCOLAIRE
    // ============================================================

    @Transactional(readOnly = true)
    public OperationComptableDTO genererRapport(
            Long ecoleId,
            Long anneeId
    ) {

        if (ecoleId == null) {
            throw new RuntimeException(
                    "L'école est obligatoire."
            );
        }

        if (anneeId == null) {
            throw new RuntimeException(
                    "L'année scolaire est obligatoire."
            );
        }

        AnneeScolaire anneeScolaire =
                trouverAnneeScolaire(anneeId);

        if (anneeScolaire.getEcole() == null
                || !ecoleId.equals(
                anneeScolaire.getEcole().getId()
        )) {

            throw new RuntimeException(
                    "Cette année scolaire n'appartient pas à cette école."
            );
        }

        // =========================================================
        // RÉCUPÉRATION DES OPÉRATIONS DE L'ANNÉE
        // =========================================================

        List<OperationComptableDTO> operations =
                operationComptableRepository
                        .findByEcole_IdAndAnneeScolaire_IdOrderByDateOperationDesc(
                                ecoleId,
                                anneeId
                        )
                        .stream()
                        .map(this::mapToDTO)
                        .toList();

        // =========================================================
        // CALCUL DES TOTAUX
        // =========================================================

        double totalRecettes = operations.stream()
                .filter(op ->
                        "RECETTE".equals(op.getNature())
                )
                .mapToDouble(op ->
                        op.getMontant() != null
                                ? op.getMontant()
                                : 0.0
                )
                .sum();

        double totalDepenses = operations.stream()
                .filter(op ->
                        "DEPENSE".equals(op.getNature())
                )
                .mapToDouble(op ->
                        op.getMontant() != null
                                ? op.getMontant()
                                : 0.0
                )
                .sum();

        double totalEmprunts = operations.stream()
                .filter(op ->
                        "EMPRUNT".equals(op.getNature())
                )
                .mapToDouble(op ->
                        op.getMontant() != null
                                ? op.getMontant()
                                : 0.0
                )
                .sum();

        double totalRemboursements = operations.stream()
                .filter(op ->
                        "REMBOURSEMENT_EMPRUNT".equals(op.getNature())
                )
                .mapToDouble(op ->
                        op.getMontant() != null
                                ? op.getMontant()
                                : 0.0
                )
                .sum();

        // =========================================================
        // TRÉSORERIE
        // =========================================================

        double solde =
                totalRecettes
                        + totalEmprunts
                        - totalDepenses
                        - totalRemboursements;

        // =========================================================
        // OBJET RACINE DU RAPPORT
        // =========================================================

        OperationComptableDTO rapport =
                new OperationComptableDTO();

        rapport.setAnneeScolaireId(anneeId);

        rapport.setTotalRecettes(totalRecettes);
        rapport.setTotalDepenses(totalDepenses);
        rapport.setTotalEmprunts(totalEmprunts);
        rapport.setTotalRemboursements(totalRemboursements);

        rapport.setSolde(solde);

        rapport.setNombreOperations(
                operations.size()
        );

        rapport.setOperations(
                operations
        );

        return rapport;
    }
    // ============================================================
// RAPPORT PAR PLAGE DE DATES
// ============================================================

    @Transactional(readOnly = true)
    public OperationComptableDTO genererRapportParPeriode(
            Long ecoleId,
            LocalDate debut,
            LocalDate fin
    ) {

        if (ecoleId == null) {
            throw new RuntimeException("L'école est obligatoire.");
        }

        if (debut == null || fin == null) {
            throw new RuntimeException(
                    "Les dates de début et de fin sont obligatoires."
            );
        }

        if (fin.isBefore(debut)) {
            throw new RuntimeException(
                    "La date de fin ne peut pas être antérieure à la date de début."
            );
        }

        LocalDateTime debutDateTime = debut.atStartOfDay();
        LocalDateTime finDateTime = fin.atTime(23, 59, 59);

        List<OperationComptableDTO> operations =
                operationComptableRepository
                        .findByEcole_IdAndDateOperationBetweenOrderByDateOperationDesc(
                                ecoleId,
                                debutDateTime,
                                finDateTime
                        )
                        .stream()
                        .map(this::mapToDTO)
                        .toList();

        return construireRapport(operations, null);
    }
    // ============================================================
    // TOUTES LES OPÉRATIONS D'UNE ÉCOLE
    // ============================================================

    @Transactional(readOnly = true)
    public List<OperationComptableDTO> getOperationsByEcole(
            Long ecoleId
    ) {

        if (ecoleId == null) {
            throw new RuntimeException(
                    "L'école est obligatoire."
            );
        }

        return operationComptableRepository
                .findByEcole_IdOrderByDateOperationDesc(ecoleId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // ============================================================
    // OPÉRATIONS PAR ANNÉE
    // ============================================================

    @Transactional(readOnly = true)
    public List<OperationComptableDTO> getOperationsByAnnee(
            Long ecoleId,
            Long anneeId
    ) {

        if (ecoleId == null) {
            throw new RuntimeException(
                    "L'école est obligatoire."
            );
        }

        if (anneeId == null) {
            throw new RuntimeException(
                    "L'année scolaire est obligatoire."
            );
        }

        AnneeScolaire anneeScolaire =
                trouverAnneeScolaire(anneeId);

        /*
         * Pas besoin de créer artificiellement une Ecole
         * avec un double-brace initializer.
         */
        if (anneeScolaire.getEcole() == null
                || !ecoleId.equals(
                anneeScolaire.getEcole().getId()
        )) {

            throw new RuntimeException(
                    "Cette année scolaire n'appartient pas à cette école."
            );
        }

        return operationComptableRepository
                .findByEcole_IdAndAnneeScolaire_IdOrderByDateOperationDesc(
                        ecoleId,
                        anneeId
                )
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // ============================================================
    // RECHERCHE ANNÉE SCOLAIRE
    // ============================================================

    private AnneeScolaire trouverAnneeScolaire(
            Long anneeId
    ) {

        return anneeScolaireRepository
                .findById(anneeId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Année scolaire introuvable : "
                                        + anneeId
                        )
                );
    }

    // ============================================================
    // VALIDATION ÉCOLE
    // ============================================================

    private void verifierEcole(
            Ecole ecole
    ) {

        if (ecole == null || ecole.getId() == null) {
            throw new RuntimeException(
                    "L'école est obligatoire."
            );
        }
    }

    // ============================================================
    // VALIDATION ANNÉE / ÉCOLE
    // ============================================================

    private void verifierAnneeAppartientEcole(
            AnneeScolaire anneeScolaire,
            Ecole ecole
    ) {

        if (anneeScolaire == null) {
            throw new RuntimeException(
                    "L'année scolaire est obligatoire."
            );
        }

        verifierEcole(ecole);

        if (anneeScolaire.getEcole() == null
                || anneeScolaire.getEcole().getId() == null) {

            throw new RuntimeException(
                    "L'année scolaire n'est associée à aucune école."
            );
        }

        if (!ecole.getId().equals(
                anneeScolaire.getEcole().getId()
        )) {

            throw new RuntimeException(
                    "L'année scolaire n'appartient pas à cette école."
            );
        }
    }

    // ============================================================
    // VALIDATION MONTANT
    // ============================================================

    private void verifierMontant(
            Double montant
    ) {

        if (montant == null || montant <= 0) {
            throw new RuntimeException(
                    "Le montant doit être supérieur à zéro."
            );
        }
    }

    // ============================================================
    // VALIDATION DATE DANS ANNÉE SCOLAIRE
    // ============================================================

    private void verifierDateDansAnnee(
            LocalDate date,
            AnneeScolaire anneeScolaire
    ) {

        if (date == null) {
            return;
        }

        /*
         * On vérifie uniquement lorsque les bornes existent.
         */
        if (anneeScolaire.getDateDebut() != null
                && date.isBefore(
                anneeScolaire.getDateDebut()
        )) {

            throw new RuntimeException(
                    "La date de l'opération est antérieure au début de l'année scolaire."
            );
        }

        if (anneeScolaire.getDateFin() != null
                && date.isAfter(
                anneeScolaire.getDateFin()
        )) {

            throw new RuntimeException(
                    "La date de l'opération est postérieure à la fin de l'année scolaire."
            );
        }
    }

    // ============================================================
    // MAPPING DTO
    // ============================================================

    private OperationComptableDTO mapToDTO(
            OperationComptable operation
    ) {

        OperationComptableDTO dto =
                new OperationComptableDTO();

        dto.setId(operation.getId());
        dto.setEcoleId(
                operation.getEcole() != null
                        ? operation.getEcole().getId()
                        : null
        );

        dto.setLibelle(
                operation.getLibelle()
        );

        dto.setMontant(
                operation.getMontant()
        );

        dto.setDateOperation(
                operation.getDateOperation()
        );

        dto.setReference(
                operation.getReference()
        );

        dto.setModePaiement(
                operation.getModePaiement()
        );

        dto.setNature(
                operation.getNature().name()
        );

        // --------------------------------------------------------
        // ANNÉE SCOLAIRE
        // --------------------------------------------------------

        if (operation.getAnneeScolaire() != null) {

            dto.setAnneeScolaireId(
                    operation.getAnneeScolaire().getId()
            );
        }

        // --------------------------------------------------------
        // CATÉGORIE
        // --------------------------------------------------------

        if (operation.getCategorieDepense() != null) {

            dto.setCategorieDepenseId(
                    operation
                            .getCategorieDepense()
                            .getId()
            );

            dto.setCategorieDepenseNom(
                    operation
                            .getCategorieDepense()
                            .getNom()
            );
        }

        // --------------------------------------------------------
        // PAIEMENT SCOLARITÉ
        // --------------------------------------------------------

        if (operation.getPaiementScolarite() != null) {

            dto.setTypeOperation(
                    "PAIEMENT_SCOLARITE"
            );

            dto.setReferenceId(
                    operation
                            .getPaiementScolarite()
                            .getId()
            );
        }

        // --------------------------------------------------------
        // PAIEMENT DÉPENSE
        // --------------------------------------------------------

        else if (operation.getPaiementDepense() != null) {

            dto.setTypeOperation(
                    "DEPENSE"
            );

            dto.setReferenceId(
                    operation
                            .getPaiementDepense()
                            .getId()
            );
        }

        // --------------------------------------------------------
        // EMPRUNT
        // --------------------------------------------------------

        else if (operation.getEmprunt() != null) {

            dto.setTypeOperation(
                    "EMPRUNT"
            );

            dto.setReferenceId(
                    operation
                            .getEmprunt()
                            .getId()
            );
        }

        // --------------------------------------------------------
        // REMBOURSEMENT EMPRUNT
        // --------------------------------------------------------

        else if (operation.getRemboursementEmprunt() != null) {

            dto.setTypeOperation(
                    "REMBOURSEMENT_EMPRUNT"
            );

            dto.setReferenceId(
                    operation
                            .getRemboursementEmprunt()
                            .getId()
            );
        }

        // --------------------------------------------------------
        // PAIEMENT ENSEIGNANT
        // --------------------------------------------------------

        else if (operation.getPaiementEnseignantId() != null) {

            dto.setTypeOperation(
                    "PAIEMENT_ENSEIGNANT"
            );

            dto.setReferenceId(
                    operation.getPaiementEnseignantId()
            );
        }

        return dto;
    }

    // ============================================================
    // MÉTHODE PUBLIQUE DE MAPPING
    // ============================================================

    public OperationComptableDTO toDto(
            OperationComptable operation
    ) {

        return mapToDTO(operation);
    }
}