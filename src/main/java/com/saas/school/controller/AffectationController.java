package com.saas.school.controller;

import com.saas.school.dto.AffectationDTO;
import com.saas.school.entity.Affectation;
import com.saas.school.repository.AffectationRepository;
import com.saas.school.service.AffectationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/affectations")
@RequiredArgsConstructor
public class AffectationController {

    private final AffectationService service;
    private final AffectationRepository repository;

    @PostMapping
    public Affectation create(@RequestBody
            AffectationDTO dto){

        return service.create(dto.getEnseignantId(), dto.getClasseId(), dto.getMatiereId(), dto.getAnneeId());
    }

    @GetMapping("/classe/{classeId}/annee/{anneeId}")
    public List<Affectation> getByClasse(
            @PathVariable Long classeId,
            @PathVariable Long anneeId
    ) {
        return service.getByClasse(classeId, anneeId);
    }

    @GetMapping("/annee/{anneeId}")
    public List<Affectation> getByAnnee(@PathVariable Long anneeId) {
        return repository.findByAnneeScolaireId(anneeId);
    }
}