package com.saas.school.controller;

import com.saas.school.dto.ClasseStatsDTO;
import com.saas.school.dto.EleveResponseDTO;
import com.saas.school.dto.SousGroupeRequest;
import com.saas.school.dto.SousGroupeResponseDTO;
import com.saas.school.entity.SousGroupe;
import com.saas.school.service.SousGroupeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sous-groupes")
@RequiredArgsConstructor
@CrossOrigin("*")
public class SousGroupeController {

    private final SousGroupeService sousGroupeService;

    @PostMapping
    public ResponseEntity<?> creer(@RequestBody SousGroupeRequest request) {
        try {
            SousGroupe sousGroupe = sousGroupeService.creerSousGroupe(request);
            return ResponseEntity.ok(sousGroupe);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/classe/{classeId}")
    public List<SousGroupeResponseDTO> getByClasse(@PathVariable Long classeId) {
        return sousGroupeService.getByClasse(classeId);
    }

    @GetMapping("/classe/{classeId}/eleves-annee-active")
    public List<EleveResponseDTO> getElevesClasseAnneeActive(@PathVariable Long classeId) {
        return sousGroupeService.getElevesClasseAnneeActive(classeId);
    }

    @PutMapping("/{sousGroupeId}/affecter/{eleveId}")
    public ResponseEntity<?> affecterEleve(
            @PathVariable Long sousGroupeId,
            @PathVariable Long eleveId
    ) {
        try {
            sousGroupeService.affecterEleve(sousGroupeId, eleveId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{sousGroupeId}/retirer/{eleveId}")
    public ResponseEntity<?> retirerEleve(
            @PathVariable Long sousGroupeId,
            @PathVariable Long eleveId
    ) {
        try {
            sousGroupeService.retirerEleve(sousGroupeId, eleveId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/ecole/{ecoleId}/annee/{anneeScolaireId}/stats")
    public List<ClasseStatsDTO> getAllClasseStats(
            @PathVariable Long ecoleId,
            @PathVariable Long anneeScolaireId
    ) {
        return sousGroupeService.getAllClasseStats(ecoleId, anneeScolaireId);
    }


// Dans SousGroupeController.java

    // 📥 Liste des sous-groupes d'une classe par année
    @GetMapping("/classe/{classeId}/annee/{anneeScolaireId}")
    public ResponseEntity<List<SousGroupeResponseDTO>> getByClasseAndAnnee(
            @PathVariable Long classeId,
            @PathVariable Long anneeScolaireId
    ) {
        return ResponseEntity.ok(sousGroupeService.getByClasseAndAnnee(classeId, anneeScolaireId));
    }
    @GetMapping("/{sousGroupeId}/eleves-annee-active")
    public List<EleveResponseDTO> getElevesSousGroupeAnneeActive(@PathVariable Long sousGroupeId) {
        return sousGroupeService.getElevesSousGroupeAnneeActive(sousGroupeId);
    }
    // 📥 Élèves de la classe pour l'année active
   }