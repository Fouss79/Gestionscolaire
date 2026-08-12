package com.saas.school.controller;

import com.saas.school.dto.ResultatEleveDTO;
import com.saas.school.service.ResultatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resultats")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ResultatController {

    private final ResultatService resultatService;

    @GetMapping("/classe/{classeId}")
    public List<ResultatEleveDTO> getResultatsClasse(
            @PathVariable Long classeId,
            @RequestParam Long anneeScolaireId,
            @RequestParam String periode
    ) {
        return resultatService.getResultatsClasse(classeId, anneeScolaireId, periode);
    }

    @GetMapping("/ecole/{ecoleId}")
    public List<ResultatEleveDTO> getResultatsEcole(
            @PathVariable Long ecoleId,
            @RequestParam Long anneeScolaireId,
            @RequestParam String periode
    ) {
        return resultatService.getResultatsEcole(ecoleId, anneeScolaireId, periode);
    }
}