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
    // 🔥 Générer automatiquement
    @PostMapping("/generer/{anneeId}")
    public String generer(@PathVariable Long anneeId) {
        service.generer(anneeId);
        return "Emploi du temps généré avec succès ✅";
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