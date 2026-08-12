package com.saas.school.controller;

import com.saas.school.dto.EnseignantRequest;
import com.saas.school.dto.EnseignantResponseDTO;
import com.saas.school.entity.Enseignant;
import com.saas.school.repository.EnseignantRepository;
import com.saas.school.service.EnseignantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enseignants")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EnseignantController {

    private final EnseignantService enseignantService;
    private final EnseignantRepository enseignantRepository;

    @PostMapping
    public ResponseEntity<?> creer(@RequestBody EnseignantRequest request) {
        try {
            Enseignant enseignant = enseignantService.creerEnseignant(request);
            return ResponseEntity.ok(enseignant);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/ecole/{ecoleId}")
    public List<EnseignantResponseDTO> getByEcole(@PathVariable Long ecoleId) {
        return enseignantService.getByEcole(ecoleId);
    }
    @GetMapping("/matiere/{id}")
    public List<Enseignant> parMatiere(@PathVariable Long id){
        return enseignantRepository.findByMatiere(id);
    }
    @GetMapping("/ecole/{ecoleId}/actifs")
    public List<EnseignantResponseDTO> getActifsByEcole(@PathVariable Long ecoleId) {
        return enseignantService.getActifsByEcole(ecoleId);
    }

    @GetMapping("/{id}")
    public EnseignantResponseDTO getById(@PathVariable Long id) {
        return enseignantService.getByEcole(enseignantService.getById(id).getEcole().getId())
                .stream()
                .filter(dto -> dto.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enseignant introuvable"));
    }

    @PutMapping("/toggle/{id}")
    public Enseignant toggleActif(@PathVariable Long id) {
        return enseignantService.toggleActif(id);
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> modifier(@PathVariable Long id, @RequestBody EnseignantRequest request) {
        try {
            Enseignant enseignant = enseignantService.modifierEnseignant(id, request);
            return ResponseEntity.ok(enseignant);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}