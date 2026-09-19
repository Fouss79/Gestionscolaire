package com.saas.school.controller;

import com.saas.school.dto.RepartitionEpreuveResponse;
import com.saas.school.entity.Inscription;
import com.saas.school.service.RepartitionEpreuveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/epreuves")
@RequiredArgsConstructor
public class RepartitionEpreuveController {

    private final RepartitionEpreuveService service;

    /**
     * Liste les élèves concernés par l'épreuve.
     */
    @GetMapping("/{epreuveId}/eleves")
    public ResponseEntity<List<Inscription>> getEleves(
            @PathVariable Long epreuveId
    ) {

        return ResponseEntity.ok(
                service.getElevesEligibles(epreuveId)
        );
    }

    /**
     * Lance la répartition automatique.
     */
    @PostMapping("/{epreuveId}/repartition")
    public ResponseEntity<List<RepartitionEpreuveResponse>> repartir(
            @PathVariable Long epreuveId
    ) {

        return ResponseEntity.ok(
                service.repartirAutomatiquement(epreuveId)
        );
    }

    /**
     * Récupère la répartition actuelle.
     */
    @GetMapping("/{epreuveId}/repartition")
    public ResponseEntity<List<RepartitionEpreuveResponse>> getRepartition(
            @PathVariable Long epreuveId
    ) {

        return ResponseEntity.ok(
                service.getRepartition(epreuveId)
        );
    }

    /**
     * Supprime la répartition.
     */
    @DeleteMapping("/{epreuveId}/repartition")
    public ResponseEntity<Void> supprimer(
            @PathVariable Long epreuveId
    ) {

        service.supprimerRepartition(epreuveId);

        return ResponseEntity.noContent().build();
    }
}