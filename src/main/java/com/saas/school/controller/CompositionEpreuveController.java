package com.saas.school.controller;

import com.saas.school.dto.CompositionEpreuveRequest;
import com.saas.school.dto.CompositionEpreuveResponse;
import com.saas.school.service.CompositionEpreuveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/epreuves")
@RequiredArgsConstructor
public class CompositionEpreuveController {

    private final CompositionEpreuveService service;

    /**
     * Liste les confirmations de composition d'une épreuve.
     */
    @GetMapping("/{epreuveId}/compositions")
    public ResponseEntity<List<CompositionEpreuveResponse>> getCompositions(
            @PathVariable Long epreuveId
    ) {
        return ResponseEntity.ok(
                service.getCompositions(epreuveId)
        );
    }

    /**
     * Modifie le statut de composition d'un élève.
     */
    @PatchMapping("/{epreuveId}/eleves/{inscriptionId}/composition")
    public ResponseEntity<CompositionEpreuveResponse> modifierStatut(
            @PathVariable Long epreuveId,
            @PathVariable Long inscriptionId,
            @Valid @RequestBody CompositionEpreuveRequest request
    ) {
        return ResponseEntity.ok(
                service.modifierStatut(
                        epreuveId,
                        inscriptionId,
                        request
                )
        );
    }

    /**
     * Remet le statut à NON_CONFIRME.
     */
    @DeleteMapping("/{epreuveId}/eleves/{inscriptionId}/composition")
    public ResponseEntity<Void> remettreNonConfirme(
            @PathVariable Long epreuveId,
            @PathVariable Long inscriptionId
    ) {
        service.remettreNonConfirme(
                epreuveId,
                inscriptionId
        );

        return ResponseEntity.noContent().build();
    }
}