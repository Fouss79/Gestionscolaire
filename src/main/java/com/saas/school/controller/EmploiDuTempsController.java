package com.saas.school.controller;

import com.saas.school.dto.EmploiDto;
import com.saas.school.entity.EmploiDuTemps;
import com.saas.school.repository.EmploiDuTempsRepository;
import com.saas.school.service.EmploiDuTempsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emploi")
@RequiredArgsConstructor
public class EmploiDuTempsController {

    private final EmploiDuTempsService service;
    private final EmploiDuTempsRepository edtRepo;
    @PostMapping
    public ResponseEntity<EmploiDuTemps> create(@RequestBody EmploiDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }
    @GetMapping("/filtre")
    public List<EmploiDuTemps> filtre(
            @RequestParam(required = false) Long classeId,
            @RequestParam(required = false) Long anneeId,
            @RequestParam(required = false) String jour
    ) {
        return service.filtrer(classeId, anneeId, jour);
    }
    @GetMapping("/filtrer")
    public List<EmploiDuTemps> filtrer(
            @RequestParam Long classeId,
            @RequestParam Long matiereId,
            @RequestParam String jour
    ) {
        return service.filtrer(classeId, matiereId, jour);
    }

    @GetMapping("/{id}")
    public EmploiDuTemps getById(@PathVariable Long id) {
        return edtRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Emploi du temps introuvable"));
    }
    // 🔥 Générer automatiquement
    @PostMapping("/generer/{anneeId}")
    public String generer(@PathVariable Long anneeId) {
        service.generer(anneeId);
        return "Emploi du temps généré avec succès ✅";
    }
    @GetMapping("/classe/{classeId}/jour/{jour}")
    public List<EmploiDuTemps> getParJour(
            @PathVariable Long classeId,
            @PathVariable String jour) {

        return service.getByJourEtClasse(jour, classeId);
    }

    // 🔥 Voir emploi du temps d'une classe
    @GetMapping("/classe/{classeId}/{anneeId}")
    public List<EmploiDuTemps> getByClasse(
            @PathVariable Long classeId,
            @PathVariable Long anneeId
    ) {
        return service.getByClasse(classeId, anneeId);
    }
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        edtRepo.deleteById(id);
    }



}