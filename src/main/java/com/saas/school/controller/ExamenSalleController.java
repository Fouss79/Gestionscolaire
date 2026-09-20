package com.saas.school.controller;

import com.saas.school.dto.ExamenSalleResponse;
import com.saas.school.service.ExamenSalleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/examens/{examenId}/salles")
@RequiredArgsConstructor
public class ExamenSalleController {

    private final ExamenSalleService service;

    /**
     * Liste les salles affectées à l'examen.
     *
     * GET /api/examens/{examenId}/salles
     */
    @GetMapping
    public ResponseEntity<List<ExamenSalleResponse>> list(
            @PathVariable Long examenId
    ) {
        return ResponseEntity.ok(service.list(examenId));
    }

    /**
     * Affecte une salle à l'examen.
     *
     * POST /api/examens/{examenId}/salles/{salleId}
     */
    @PostMapping("/{salleId}")
    public ResponseEntity<ExamenSalleResponse> affecter(
            @PathVariable Long examenId,
            @PathVariable Long salleId
    ) {
        return ResponseEntity.ok(
                service.affecter(examenId, salleId)
        );
    }

    /**
     * Retire une salle de l'examen.
     *
     * DELETE /api/examens/{examenId}/salles/{salleId}
     */
    @DeleteMapping("/{salleId}")
    public ResponseEntity<Void> retirer(
            @PathVariable Long examenId,
            @PathVariable Long salleId
    ) {
        service.retirer(examenId, salleId);
        return ResponseEntity.noContent().build();
    }
}