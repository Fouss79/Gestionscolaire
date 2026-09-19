package com.saas.school.controller;

import com.saas.school.dto.EpreuveEnseignantDto;
import com.saas.school.service.EpreuveEnseignantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EpreuveEnseignantController {

    private final EpreuveEnseignantService epreuveEnseignantService;

    @PostMapping("/api/epreuve-enseignants")
    @ResponseStatus(HttpStatus.CREATED)
    public EpreuveEnseignantDto.Response add(@Valid @RequestBody EpreuveEnseignantDto.Request request) {
        return epreuveEnseignantService.add(request);
    }

    @DeleteMapping("/api/epreuve-enseignants/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) {
        epreuveEnseignantService.remove(id);
    }

    @GetMapping("/api/epreuves/{epreuveId}/enseignants")
    public List<EpreuveEnseignantDto.Response> listByEpreuve(@PathVariable Long epreuveId) {
        return epreuveEnseignantService.listByEpreuve(epreuveId);
    }
}
