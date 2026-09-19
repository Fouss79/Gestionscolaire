package com.saas.school.controller;

import com.saas.school.dto.CreneauExamenRequest;
import com.saas.school.dto.CreneauExamenResponse;
import com.saas.school.service.CreneauExamenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CreneauExamenController {

    private final CreneauExamenService creneauExamenService;

    @PostMapping("/api/creneaux")
    @ResponseStatus(HttpStatus.CREATED)
    public CreneauExamenResponse create(@Valid @RequestBody CreneauExamenRequest request) {
        return creneauExamenService.create(request);
    }

    @PutMapping("/api/creneaux/{id}")
    public CreneauExamenResponse update(@PathVariable Long id, @Valid @RequestBody CreneauExamenRequest request) {
        return creneauExamenService.update(id, request);
    }

    @DeleteMapping("/api/creneaux/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        creneauExamenService.delete(id);
    }

    @GetMapping("/api/examens/{examenId}/creneaux")
    public List<CreneauExamenResponse> listByExamen(@PathVariable Long examenId) {
        return creneauExamenService.listByExamen(examenId);
    }
}
