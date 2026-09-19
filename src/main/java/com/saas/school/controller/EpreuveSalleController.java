package com.saas.school.controller;

import com.saas.school.dto.EpreuveSalleResponse;
import com.saas.school.service.EpreuveSalleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class EpreuveSalleController {

    private final EpreuveSalleService epreuveSalleService;

    /**
     * Liste les salles affectées à une épreuve.
     *
     * GET /api/epreuves/{epreuveId}/salles
     */
    @GetMapping("/api/epreuves/{epreuveId}/salles")
    public List<EpreuveSalleResponse> getByEpreuve(
            @PathVariable Long epreuveId
    ) {

        return epreuveSalleService.getByEpreuve(epreuveId)
                .stream()
                .map(EpreuveSalleResponse::from)
                .toList();
    }

    /**
     * Affecte une salle à une épreuve.
     *
     * POST /api/epreuves/{epreuveId}/salles/{salleId}
     */
    @PostMapping("/api/epreuves/{epreuveId}/salles/{salleId}")
    @ResponseStatus(HttpStatus.CREATED)
    public EpreuveSalleResponse affecter(
            @PathVariable Long epreuveId,
            @PathVariable Long salleId
    ) {

        return EpreuveSalleResponse.from(
                epreuveSalleService.affecter(
                        epreuveId,
                        salleId
                )
        );
    }

    /**
     * Retire une salle d'une épreuve.
     *
     * DELETE /api/epreuves/{epreuveId}/salles/{salleId}
     */
    @DeleteMapping("/api/epreuves/{epreuveId}/salles/{salleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirer(
            @PathVariable Long epreuveId,
            @PathVariable Long salleId
    ) {

        epreuveSalleService.retirer(
                epreuveId,
                salleId
        );
    }
}