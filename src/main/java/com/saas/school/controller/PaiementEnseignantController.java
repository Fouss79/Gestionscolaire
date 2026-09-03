package com.saas.school.controller;

import com.saas.school.dto.PaiementEnseignantDTO;
import com.saas.school.service.PaiementEnseignantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PaiementEnseignantController {

    private final PaiementEnseignantService paiementService;

    @GetMapping("/previsualiser")
    public ResponseEntity<List<PaiementEnseignantDTO>> previsualiser(
            @RequestParam LocalDate debut,
            @RequestParam LocalDate fin,
            @RequestParam Long anneeId) {
        return ResponseEntity.ok(paiementService.previsualiserTous(debut, fin, anneeId));
    }

    @PostMapping("/generer")
    public ResponseEntity<List<PaiementEnseignantDTO>> generer(
            @RequestParam LocalDate debut,
            @RequestParam LocalDate fin,
            @RequestParam Long anneeId) {
        return ResponseEntity.ok(paiementService.genererPaiements(debut, fin, anneeId));
    }

    @PutMapping("/{id}/payer")
    public ResponseEntity<PaiementEnseignantDTO> payer(@PathVariable Long id) {
        return ResponseEntity.ok(paiementService.marquerPaye(id));
    }

    @GetMapping
    public ResponseEntity<List<PaiementEnseignantDTO>> lister(@RequestParam Long anneeId) {
        return ResponseEntity.ok(paiementService.listerPaiements(anneeId));
    }
}