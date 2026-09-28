
package com.saas.school.controller;

import com.saas.school.dto.BulletinDtos;
import com.saas.school.dto.ResultatEleveDTO;
import com.saas.school.service.ResultatPrimaireService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resultats/primaire")
@RequiredArgsConstructor
public class ResultatPrimaireController {

    private final ResultatPrimaireService resultatPrimaireService;

    /**
     * Résultats mensuels d'une classe primaire.
     *
     * GET /api/resultats/primaire/classe/10
     *     ?anneeScolaireId=2&mois=Septembre
     */
    @GetMapping("/classe/{classeId}")
    public ResponseEntity<List<ResultatEleveDTO>> getResultatsClasse(
            @PathVariable Long classeId,
            @RequestParam Long anneeScolaireId,
            @RequestParam String mois
    ) {

        List<ResultatEleveDTO> resultats =
                resultatPrimaireService.getResultatsClasse(
                        classeId,
                        anneeScolaireId,
                        mois
                );

        return ResponseEntity.ok(resultats);
    }

    /**
     * Résultats mensuels de toute une école.
     *
     * GET /api/resultats/primaire/ecole/52
     *     ?anneeScolaireId=2&mois=Septembre
     */
    @GetMapping("/ecole/{ecoleId}")
    public ResponseEntity<List<ResultatEleveDTO>> getResultatsEcole(
            @PathVariable Long ecoleId,
            @RequestParam Long anneeScolaireId,
            @RequestParam String mois
    ) {

        List<ResultatEleveDTO> resultats =
                resultatPrimaireService.getResultatsEcole(
                        ecoleId,
                        anneeScolaireId,
                        mois
                );

        return ResponseEntity.ok(resultats);
    }

    /**
     * Bulletin détaillé d'un élève du primaire.
     *
     * GET /api/resultats/primaire/eleve/120
     *     ?classeId=10
     *     &anneeScolaireId=2
     *     &mois=Septembre
     */
    @GetMapping("/eleve/{inscriptionId}")
    public ResponseEntity<BulletinDtos> getBulletinEleve(
            @PathVariable Long inscriptionId,
            @RequestParam Long classeId,
            @RequestParam Long anneeScolaireId,
            @RequestParam String mois
    ) {

        BulletinDtos bulletin =
                resultatPrimaireService.getBulletinEleve(
                        inscriptionId,
                        classeId,
                        anneeScolaireId,
                        mois
                );

        return ResponseEntity.ok(bulletin);
    }
}