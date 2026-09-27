package com.saas.school.controller;


import com.saas.school.dto.*;
import com.saas.school.entity.Inscription;
import com.saas.school.service.ReinscriptionPrimaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reinscriptions-primaire")
@RequiredArgsConstructor
public class ReinscriptionPrimaireController {

    private final ReinscriptionPrimaireService service;

    @GetMapping("/ecole/{ecoleId}")
    public List<ReinscriptionPrimaireDto> lister(
            @PathVariable Long ecoleId
    ) {
        return service.lister(ecoleId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReinscriptionPrimaireDtoPlaceholder reinscrire(
            @RequestBody ReinscriptionPrimaireRequest request
    ) {
        Inscription inscription = service.reinscrire(request);

        return new ReinscriptionPrimaireDtoPlaceholder(
                inscription.getId(),
                inscription.getEleve().getId(),
                inscription.getClasse().getId(),
                inscription.getStatut().name()
        );
    }

    public record ReinscriptionPrimaireDtoPlaceholder(
            Long inscriptionId,
            Long eleveId,
            Long classeId,
            String statut
    ) {}
}