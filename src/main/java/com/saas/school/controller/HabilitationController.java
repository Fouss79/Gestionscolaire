package com.saas.school.controller;

import com.saas.school.dto.HabilitationDto;
import com.saas.school.entity.Enseignant;
import com.saas.school.entity.Habilitation;
import com.saas.school.service.HabilitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/habilitations")
@RequiredArgsConstructor
public class HabilitationController {

    private final HabilitationService service;

    // 🔥 CREATE
    @PostMapping
    public ResponseEntity<Habilitation> create(@RequestBody HabilitationDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    // 🔥 ENSEIGNANTS PAR MATIERE + ANNEE
    @GetMapping("/matiere/{matiereId}/annee/{anneeId}")
    public List<Enseignant> getEnseignantsByMatiere(
            @PathVariable Long matiereId,
            @PathVariable Long anneeId
    ) {
        return service.getEnseignantsByMatiere(matiereId, anneeId);
    }

    // 🔥 ALL HABILITATIONS


    // 🔥 Ecole (via service maintenant)
    @GetMapping("/ecole/{ecoleId}")
    public List<Habilitation> getByEcole(@PathVariable Long ecoleId) {
        return service.getByEcole(ecoleId);
    }
    @GetMapping("/ecole/{ecoleId}/annee/{anneeId}")
    public List<Habilitation> getByEcoleAndAnnee(
            @PathVariable Long ecoleId,
            @PathVariable Long anneeId) {

        return service.getByEcoleAndAnnee(ecoleId, anneeId);
    }
}