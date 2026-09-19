package com.saas.school.controller;

import com.saas.school.dto.ExamenRequest;
import com.saas.school.dto.ExamenResponse;
import com.saas.school.service.ExamenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/examens")
@RequiredArgsConstructor
public class ExamenController {

    private final ExamenService examenService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamenResponse create(@Valid @RequestBody ExamenRequest request) {
        return examenService.create(request);
    }

    @PutMapping("/{id}")
    public ExamenResponse update(@PathVariable Long id, @Valid @RequestBody ExamenRequest request) {
        return examenService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        examenService.delete(id);
    }

    @GetMapping("/{id}")
    public ExamenResponse get(@PathVariable Long id) {
        return examenService.get(id);
    }

    @GetMapping("/ecole/{ecoleId}")
    public List<ExamenResponse> listByEcole(@PathVariable Long ecoleId) {
        return examenService.listByEcole(ecoleId);
    }

    @GetMapping("/ecole/{ecoleId}/annee/{anneeScolaireId}")
    public List<ExamenResponse> listByEcoleAndAnnee(@PathVariable Long ecoleId, @PathVariable Long anneeScolaireId) {
        return examenService.listByEcoleAndAnnee(ecoleId, anneeScolaireId);
    }
}
