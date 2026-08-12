package com.saas.school.controller;

import com.saas.school.dto.LigneFraisDTO;
import com.saas.school.entity.LigneFrais;
import com.saas.school.service.LigneFraisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ligne-frais")
@RequiredArgsConstructor
@CrossOrigin("*")
public class LigneFraisController {

    private final LigneFraisService ligneFraisService;

    // Toutes les lignes de frais d'une école
    @GetMapping("/ecole/{ecoleId}")
    public List<LigneFraisDTO> getByEcole(@PathVariable Long ecoleId) {
        return ligneFraisService.getByEcole(ecoleId);
    }

    // Toutes les lignes de frais d'une inscription
    @GetMapping("/inscription/{inscriptionId}")
    public List<LigneFrais> getByInscription(@PathVariable Long inscriptionId) {
        return ligneFraisService.getByInscription(inscriptionId);
    }

    // Une ligne de frais par son id
    @GetMapping("/{id}")
    public LigneFrais getById(@PathVariable Long id) {
        return ligneFraisService.getById(id);
    }

    // Ligne de frais correspondant à un type de frais
    @GetMapping("/inscription/{inscriptionId}/type/{codeTypeFrais}")
    public LigneFrais getByInscriptionAndType(
            @PathVariable Long inscriptionId,
            @PathVariable String codeTypeFrais
    ) {
        return ligneFraisService.getByInscriptionAndType(inscriptionId, codeTypeFrais);
    }

    // Suppression
    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Long id) {
        ligneFraisService.supprimer(id);
    }
}