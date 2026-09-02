package com.saas.school.controller;

import com.saas.school.dto.OperationComptableDTO;
import com.saas.school.entity.CategorieDepense;
import com.saas.school.entity.Depense;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.OperationComptable;
import com.saas.school.repository.CategorieDepenseRepository;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.service.OperationComptableService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operations-comptables")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OperationComptableController {

    private final OperationComptableService operationComptableService;
    private final EcoleRepository ecoleRepository;
    private final CategorieDepenseRepository categorieDepenseRepository;


    // =========================================================
    // RAPPORT COMPTABLE GLOBAL
    // =========================================================

    @GetMapping("/rapport/{ecoleId}")
    public ResponseEntity<OperationComptableDTO> genererRapport(
            @PathVariable Long ecoleId
    ) {

        OperationComptableDTO rapport =
                operationComptableService.genererRapport(ecoleId);

        return ResponseEntity.ok(rapport);
    }


    // =========================================================
    // CRÉER UNE DÉPENSE
    // =========================================================

    @PostMapping("/depenses")
    public ResponseEntity<OperationComptable> creerDepense(
            @RequestParam Long ecoleId,
            @RequestParam Double montant,
            @RequestParam String libelle,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String reference,
            @RequestParam Long categorieId
    ) {
        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        CategorieDepense categorie = categorieDepenseRepository.findById(categorieId)
                .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));

        Depense depense = new Depense();
        depense.setEcole(ecole);
        depense.setMontant(montant);
        depense.setLibelle(libelle);
        depense.setDescription(description);
        depense.setCategorie(categorie);

        return ResponseEntity.ok(
                operationComptableService.creerDepense(depense)
        );
    }

    // =========================================================
    // CONVERSION OPÉRATION → DTO
    // =========================================================

    private OperationComptableDTO convertirEnDTO(
            OperationComptable operation
    ) {

        OperationComptableDTO dto =
                new OperationComptableDTO();

        dto.setId(operation.getId());

        if (operation.getEcole() != null) {
            dto.setEcoleId(
                    operation.getEcole().getId()
            );
        }

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

        if (operation.getNature() != null) {
            dto.setNature(
                    operation.getNature().name()
            );
        }

        dto.setTypeOperation(
                "DEPENSE"
        );

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

        return dto;
    }


    // =========================================================
    // DTO REQUÊTE DÉPENSE
    // =========================================================

    @Data
    public static class DepenseRequestDTO {

        private Double montant;

        private String libelle;

        private String reference;

        private String modePaiement;

        private Long categorieDepenseId;
    }
}
