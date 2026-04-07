package com.saas.school.controller;

import com.saas.school.dto.MatiereclasseDTO;
import com.saas.school.entity.MatiereClasse;
import com.saas.school.service.MatiereClasseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matiereclasse")
@RequiredArgsConstructor
@CrossOrigin
public class MatiereClasseController {

    private final MatiereClasseService service;

    @PostMapping
    public MatiereClasse create(@RequestBody MatiereclasseDTO mc) {
        return service.create(mc);
    }
    @GetMapping("/classe/{classeId}/annee/{anneeId}")
    public List<MatiereClasse> getByClasseAndAnnee(
            @PathVariable Long classeId,
            @PathVariable Long anneeId
    ) {
        return service.getByClasseAndAnnee(classeId, anneeId);
    }

    @GetMapping("/coef")
    public int getCoef(
            @RequestParam Long matiereId,
            @RequestParam Long classeId,
            @RequestParam Long anneeId
    ) {
        return service.getCoefficient(matiereId, classeId, anneeId);
    }
}