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
    @GetMapping("/matiere/{matiereId}")
    public List<Enseignant> getEnseignantsByMatiere(
            @PathVariable Long matiereId
    ) {
        return service.getEnseignantsByMatiere(matiereId);
    }

    // 🔥 ALL HABILITATIONS


    // 🔥 Ecole (via service maintenant)
    @GetMapping("/ecole/{ecoleId}")
    public List<Habilitation> getByEcole(@PathVariable Long ecoleId) {
        return service.getByEcole(ecoleId);
    }

}