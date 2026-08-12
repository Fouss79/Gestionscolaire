package com.saas.school.controller;

import com.saas.school.dto.TarifResponseDTO;
import com.saas.school.entity.Tarif;
import com.saas.school.service.TarifService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tarifs")
@RequiredArgsConstructor
@CrossOrigin("*")
public class TarifController {

    private final TarifService tarifService;

    @PostMapping
    public Tarif creerOuModifier(
            @RequestParam Long ecoleId,
            @RequestParam Long niveauId,
            @RequestParam Long anneeId,
            @RequestParam String codeTypeFrais,
            @RequestParam Double montant
    ) {
        return tarifService.creerOuModifierTarif(
                ecoleId, niveauId, anneeId, codeTypeFrais, montant
        );
    }

    @GetMapping("/ecole/{ecoleId}")
    public List<TarifResponseDTO> getByEcole(@PathVariable Long ecoleId) {
        return tarifService.getByEcole(ecoleId);
    }

    @GetMapping("/ecole/{ecoleId}/annee/{anneeId}")
    public List<TarifResponseDTO> getByEcoleAndAnnee(
            @PathVariable Long ecoleId,
            @PathVariable Long anneeId
    ) {
        return tarifService.getByEcoleAndAnnee(ecoleId, anneeId);
    }

    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Long id) {
        tarifService.supprimer(id);
    }
}