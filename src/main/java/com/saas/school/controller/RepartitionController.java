package com.saas.school.controller;

import com.saas.school.dto.AffectationEleveExamenResponse;
import com.saas.school.dto.RepartitionPreviewResponse;
import com.saas.school.dto.RepartitionResultResponse;
import com.saas.school.service.RepartitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/examens/{examenId}")
@RequiredArgsConstructor
public class RepartitionController {

    private final RepartitionService repartitionService;

    // Aperçu avant confirmation : nombre d'élèves, capacité totale, répartition prévue.
    @GetMapping("/repartition/apercu")
    public RepartitionPreviewResponse apercu(@PathVariable Long examenId) {
        return repartitionService.preview(examenId);
    }

    // Génère (ou régénère) la répartition. Transactionnel : tout ou rien.
    @PostMapping("/repartition")
    public RepartitionResultResponse generer(@PathVariable Long examenId) {
        return repartitionService.genererRepartition(examenId);
    }

    @GetMapping("/affectations")
    public List<AffectationEleveExamenResponse> affectations(@PathVariable Long examenId) {
        return repartitionService.getAffectations(examenId);
    }

    @GetMapping("/affectations/salle/{salleId}")
    public List<AffectationEleveExamenResponse> affectationsParSalle(
            @PathVariable Long examenId, @PathVariable Long salleId) {
        return repartitionService.getAffectationsBySalle(examenId, salleId);
    }

    @GetMapping("/affectations/eleve/{inscriptionId}")
    public AffectationEleveExamenResponse affectationEleve(
            @PathVariable Long examenId, @PathVariable Long inscriptionId) {
        return repartitionService.getAffectationByEleve(examenId, inscriptionId);
    }
}
