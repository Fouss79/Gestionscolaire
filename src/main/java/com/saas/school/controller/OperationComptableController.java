package com.saas.school.controller;

import com.saas.school.dto.OperationComptableDTO;
import com.saas.school.dto.RecetteRequestDTO;
import com.saas.school.entity.Ecole;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.service.OperationComptableService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operations-comptables")
@RequiredArgsConstructor
@CrossOrigin("*")
public class OperationComptableController {

    private final OperationComptableService operationComptableService;
    private final EcoleRepository ecoleRepository;

    @GetMapping("/rapport/{ecoleId}")
    public OperationComptableDTO getRapport(@PathVariable Long ecoleId) {
        return operationComptableService.genererRapport(ecoleId);
    }

    @PostMapping("/recette/ecole/{ecoleId}")
    public OperationComptableDTO creerRecette(
            @PathVariable Long ecoleId,
            @RequestBody RecetteRequestDTO dto
    ) {

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        var operation = operationComptableService.creerRecette(
                ecole,
                dto.getMontant(),
                dto.getLibelle(),
                dto.getReference(),
                dto.getModePaiement()
        );

        return operationComptableService.toDto(operation);
    }
}