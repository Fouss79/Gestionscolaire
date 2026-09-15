package com.saas.school.controller;

import com.saas.school.dto.OperationComptableDTO;
import com.saas.school.dto.RecetteRequestDTO;
import com.saas.school.entity.Ecole;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.service.OperationComptableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operations-comptables")
@RequiredArgsConstructor
@CrossOrigin("*")
public class OperationComptableController {

    private final OperationComptableService operationComptableService;
    private final EcoleRepository ecoleRepository;


    // =========================================================
    // RAPPORT COMPTABLE D'UNE ANNÉE SCOLAIRE
    // =========================================================


    @GetMapping("/rapport/{ecoleId}")
    public OperationComptableDTO getRapport(
            @PathVariable Long ecoleId,
            @RequestParam Long anneeId
    ) {
        return operationComptableService.genererRapport(
                ecoleId,
                anneeId
        );
    }

    // =========================================================
    // CRÉER UNE RECETTE MANUELLE
    // =========================================================

    @PostMapping("/recette/ecole/{ecoleId}")
    public OperationComptableDTO creerRecette(
            @PathVariable Long ecoleId,
            @RequestBody RecetteRequestDTO dto
    ) {

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "École introuvable"
                        )
                );

        if (dto.getAnneeScolaireId() == null) {
            throw new RuntimeException(
                    "L'année scolaire est obligatoire"
            );
        }

        var operation =
                operationComptableService.creerRecette(
                        ecole,
                        dto.getMontant(),
                        dto.getLibelle(),
                        dto.getReference(),
                        dto.getModePaiement(),
                        dto.getDateRecette(),
                        dto.getAnneeScolaireId()
                );

        return operationComptableService.toDto(
                operation
        );
    }
}