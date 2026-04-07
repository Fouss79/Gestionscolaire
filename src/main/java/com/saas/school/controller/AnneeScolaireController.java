package com.saas.school.controller;

import com.saas.school.dto.AnneeRequestDTO;
import com.saas.school.entity.AnneeScolaire;
import com.saas.school.service.AnneeScolaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/annees")
@RequiredArgsConstructor
public class AnneeScolaireController {

    private final AnneeScolaireService service;

    // 🔥 créer année
    @PostMapping
    public ResponseEntity<AnneeScolaire> create(
            @RequestBody AnneeRequestDTO dto
    ) {
        return ResponseEntity.ok(service.creer(dto.getNom(), dto.getEcoleId()));
    }

    // 📥 toutes les années
    @GetMapping("/ecole/{ecoleId}")
    public ResponseEntity<List<AnneeScolaire>> getByEcole(@PathVariable Long ecoleId) {
        return ResponseEntity.ok(service.getByEcole(ecoleId));
    }

    // 🔥 activer année
    @PutMapping("/activer/{id}")
    public ResponseEntity<AnneeScolaire> activer(@PathVariable Long id) {
        return ResponseEntity.ok(service.activer(id));
    }

    // 📥 année active
    @GetMapping("/active/{ecoleId}")
    public ResponseEntity<AnneeScolaire> getActive(@PathVariable Long ecoleId) {
        return ResponseEntity.ok(service.getActive(ecoleId));
    }
}
