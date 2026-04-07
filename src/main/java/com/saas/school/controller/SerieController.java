package com.saas.school.controller;

import com.saas.school.dto.Request;
import com.saas.school.entity.Niveau;
import com.saas.school.entity.Serie;
import com.saas.school.service.NiveauService;
import com.saas.school.service.SerieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
public class SerieController {

    private final SerieService serieService;

    // 🔥 Créer niveau
    @PostMapping
    public ResponseEntity<Serie> create(@RequestBody Request request) {
        Serie serie = serieService.creerSerie(
                request.getNom(),
                request.getEcoleId()
        );
        return ResponseEntity.ok(serie);
    }

    // 📥 Tous les niveaux d'une école
    @GetMapping("/ecole/{ecoleId}")
    public ResponseEntity<List<Serie>> getByEcole(@PathVariable Long ecoleId) {
        return ResponseEntity.ok(serieService.getSeriesByEcole(ecoleId));
    }

    // 📥 Niveau par ID
    @GetMapping("/{id}")
    public ResponseEntity<Serie> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serieService.getById(id));
    }

    // ❌ Supprimer
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        serieService.delete(id);
        return ResponseEntity.ok("serie supprimée");
    }
}