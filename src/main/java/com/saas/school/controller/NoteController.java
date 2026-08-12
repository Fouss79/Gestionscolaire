package com.saas.school.controller;

import com.saas.school.dto.*;
import com.saas.school.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@CrossOrigin("*")
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    public ResponseEntity<?> creerOuMettreAJour(@RequestBody NoteRequest request) {
        try {
            // ⚠️ passe par le mapping DTO, pas l'entité brute
            var note = noteService.creerOuMettreAJour(request);
            return ResponseEntity.ok(noteService.toDto(note));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/en-masse")
    public ResponseEntity<?> enregistrerEnMasse(@RequestBody NotesEnMasseRequest request) {
        try {
            // ⚠️ mappe chaque Note en NoteResponseDTO avant de renvoyer
            List<NoteResponseDTO> resultats = noteService.enregistrerEnMasse(request)
                    .stream()
                    .map(noteService::toDto)
                    .toList();
            return ResponseEntity.ok(resultats);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/classe")
    public List<NoteResponseDTO> getByClasseMatierePeriode(
            @RequestParam Long classeId,
            @RequestParam Long coefficientMatiereId,
            @RequestParam String periode,
            @RequestParam(required = false) Long sousGroupeId
    ) {
        return noteService.getByClasseMatierePeriode(classeId, coefficientMatiereId, periode, sousGroupeId);
    }

    @GetMapping
    public List<NoteResponseDTO> getByInscriptionEtPeriode(
            @RequestParam Long inscriptionId,
            @RequestParam String periode
    ) {
        return noteService.getByInscriptionEtPeriode(inscriptionId, periode);
    }

    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Long id) {
        noteService.supprimer(id);
    }
}