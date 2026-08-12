package com.saas.school.controller;

import com.saas.school.entity.Periode;
import com.saas.school.service.PeriodeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/periodes")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PeriodeController {

    private final PeriodeService periodeService;

    @Data
    static class PeriodeRequest {
        private String nom;
        private Integer ordre;
        private LocalDate dateDebut;
        private LocalDate dateFin;
        private Long anneeScolaireId;
        private Long ecoleId;
    }

    @PostMapping
    public ResponseEntity<?> creer(@RequestBody PeriodeRequest req) {
        try {
            Periode periode = periodeService.creer(
                    req.getNom(), req.getOrdre(), req.getDateDebut(), req.getDateFin(),
                    req.getAnneeScolaireId(), req.getEcoleId()
            );
            return ResponseEntity.ok(periode);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/ecole/{ecoleId}/annee/{anneeScolaireId}")
    public List<Periode> getByEcoleEtAnnee(
            @PathVariable Long ecoleId,
            @PathVariable Long anneeScolaireId
    ) {
        return periodeService.getByEcoleEtAnnee(ecoleId, anneeScolaireId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modifier(@PathVariable Long id, @RequestBody PeriodeRequest req) {
        try {
            Periode periode = periodeService.modifier(
                    id, req.getNom(), req.getOrdre(), req.getDateDebut(), req.getDateFin()
            );
            return ResponseEntity.ok(periode);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Long id) {
        periodeService.supprimer(id);
    }
}