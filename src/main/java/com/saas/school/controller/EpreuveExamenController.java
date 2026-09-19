package com.saas.school.controller;

import com.saas.school.dto.EpreuveExamenRequest;
import com.saas.school.dto.EpreuveExamenResponse;
import com.saas.school.service.EpreuveExamenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EpreuveExamenController {

    private final EpreuveExamenService epreuveExamenService;

    @PostMapping("/api/epreuves")
    @ResponseStatus(HttpStatus.CREATED)
    public EpreuveExamenResponse add(@Valid @RequestBody EpreuveExamenRequest request) {
        return epreuveExamenService.add(request);
    }

    @PutMapping("/api/epreuves/{id}")
    public EpreuveExamenResponse update(@PathVariable Long id, @Valid @RequestBody EpreuveExamenRequest request) {
        return epreuveExamenService.update(id, request);
    }

    @DeleteMapping("/api/epreuves/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        epreuveExamenService.delete(id);
    }

    @GetMapping("/api/examens/{examenId}/epreuves")
    public List<EpreuveExamenResponse> listByExamen(@PathVariable Long examenId) {
        return epreuveExamenService.listByExamen(examenId);
    }
}
