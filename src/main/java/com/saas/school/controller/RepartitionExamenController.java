package com.saas.school.controller;

import com.saas.school.dto.EleveRepartitionResponse;
import com.saas.school.dto.RepartitionExamenResponse;
import com.saas.school.dto.RepartitionSalleGroupResponse;
import com.saas.school.entity.Inscription;
import com.saas.school.service.RepartitionExamenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/examens")
@RequiredArgsConstructor
public class RepartitionExamenController {

    private final RepartitionExamenService service;

    /**
     * Liste les élèves concernés par l'examen (toutes classes confondues).
     *
     * GET /api/examens/{examenId}/eleves
     */
    @GetMapping("/{examenId}/eleves")
    public ResponseEntity<List<EleveRepartitionResponse>> getEleves(
            @PathVariable Long examenId
    ) {
        List<EleveRepartitionResponse> response =
                service.getElevesEligibles(examenId)
                        .stream()
                        .map(EleveRepartitionResponse::from)
                        .toList();

        return ResponseEntity.ok(response);
    }
    /**
     * Lance la répartition automatique pour tout l'examen.
     * Remplace toute répartition existante.
     *
     * POST /api/examens/{examenId}/repartition
     */
    @PostMapping("/{examenId}/repartition")
    public ResponseEntity<List<RepartitionExamenResponse>> repartir(
            @PathVariable Long examenId
    ) {
        return ResponseEntity.ok(service.repartirAutomatiquement(examenId));
    }

    /**
     * Répartition complète, à plat (une ligne par élève).
     *
     * GET /api/examens/{examenId}/repartition
     */
    @GetMapping("/{examenId}/repartition")
    public ResponseEntity<List<RepartitionExamenResponse>> getRepartition(
            @PathVariable Long examenId
    ) {
        return ResponseEntity.ok(service.getRepartition(examenId));
    }

    /**
     * Répartition groupée par salle (une entrée par salle, avec la
     * liste de ses élèves). Pratique pour l'affichage et l'impression
     * des feuilles de salle.
     *
     * GET /api/examens/{examenId}/repartition/par-salle
     */
    @GetMapping("/{examenId}/repartition/par-salle")
    public ResponseEntity<List<RepartitionSalleGroupResponse>> getRepartitionParSalle(
            @PathVariable Long examenId
    ) {
        return ResponseEntity.ok(
                service.getRepartitionGroupeeParSalle(examenId)
        );
    }

    /**
     * Répartition filtrée pour une épreuve donnée (classes concernées
     * par le niveau/série du programme, avec filtrage sous-groupe).
     * Pratique pour générer une feuille de présence par épreuve.
     *
     * GET /api/examens/{examenId}/epreuves/{epreuveId}/repartition
     */
    @GetMapping("/{examenId}/epreuves/{epreuveId}/repartition")
    public ResponseEntity<List<RepartitionExamenResponse>> getRepartitionParEpreuve(
            @PathVariable Long examenId,
            @PathVariable Long epreuveId
    ) {
        return ResponseEntity.ok(
                service.getRepartitionParEpreuve(examenId, epreuveId)
        );
    }

    /**
     * Supprime la répartition de l'examen.
     *
     * DELETE /api/examens/{examenId}/repartition
     */
    @DeleteMapping("/{examenId}/repartition")
    public ResponseEntity<Void> supprimer(
            @PathVariable Long examenId
    ) {
        service.supprimerRepartition(examenId);
        return ResponseEntity.noContent().build();
    }
}