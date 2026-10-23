
package com.saas.school.controller;

import com.saas.school.dto.ConduiteEnregistrementDTO;
import com.saas.school.dto.ConduiteResultatDTO;
import com.saas.school.dto.ConduiteSaisieDTO;
import com.saas.school.entity.EvaluationConduite;
import com.saas.school.service.EvaluationConduiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conduites")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTEUR', 'SECRETAIRE')")
public class EvaluationConduiteController {

    private final EvaluationConduiteService conduiteService;

    @GetMapping
    public List<ConduiteSaisieDTO> lister(
            @RequestParam Long classeId,
            @RequestParam Long anneeScolaireId,
            @RequestParam String periode
    ) {
        return conduiteService.lister(
                classeId, anneeScolaireId, periode
        );
    }

    @PostMapping
    public ConduiteResultatDTO enregistrer(
            @RequestBody ConduiteEnregistrementDTO dto
    ) {
        EvaluationConduite e = conduiteService.enregistrer(dto);

        return new ConduiteResultatDTO(
                e.getId(),
                e.getInscription().getId(),
                e.getPeriode(),
                e.getNote(),
                e.getAppreciation()
        );
    }
}
