package com.saas.school.controller;

import com.saas.school.dto.AffectationEnseignantRequest;
import com.saas.school.dto.AffectationEnseignantResponseDTO;
import com.saas.school.entity.AffectationEnseignant;
import com.saas.school.service.AffectationEnseignantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/api/affectations-enseignants")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AffectationEnseignantController {

    private final AffectationEnseignantService affectationService;

    @PostMapping
    public ResponseEntity<?> creer(@RequestBody AffectationEnseignantRequest request) {
        try {
            AffectationEnseignant affectation = affectationService.creer(request);
            return ResponseEntity.ok(affectation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/classe/{classeId}")
    public List<AffectationEnseignantResponseDTO> getByClasse(
            @PathVariable Long classeId,
            @RequestParam Long anneeScolaireId
    ) {
        return affectationService.getByClasse(classeId, anneeScolaireId);
    }

    @GetMapping("/enseignant/{enseignantId}")
    public List<AffectationEnseignantResponseDTO> getByEnseignant(
            @PathVariable Long enseignantId,
            @RequestParam Long anneeScolaireId
    ) {
        return affectationService.getByEnseignant(enseignantId, anneeScolaireId);
    }

    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Long id) {
        affectationService.supprimer(id);
    }




}