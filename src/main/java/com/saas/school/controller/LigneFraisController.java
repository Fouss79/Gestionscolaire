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

    // Suppression
    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Long id) {
        ligneFraisService.supprimer(id);
    }

    @GetMapping("/inscription/{inscriptionId}")
    public List<LigneFraisDTO> getByInscription(@PathVariable Long inscriptionId) {
        return ligneFraisService.getByInscriptionDTO(inscriptionId);
    }

    @GetMapping("/{id}")
    public LigneFraisDTO getById(@PathVariable Long id) {
        return ligneFraisService.getByIdDTO(id);
    }

    @GetMapping("/inscription/{inscriptionId}/type/{codeTypeFrais}")
    public LigneFraisDTO getByInscriptionAndType(
            @PathVariable Long inscriptionId,
            @PathVariable String codeTypeFrais
    ) {
        return ligneFraisService.getByInscriptionAndTypeDTO(inscriptionId, codeTypeFrais);
    }
}