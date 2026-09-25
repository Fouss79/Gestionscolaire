package com.saas.school.controller;

import com.saas.school.dto.NotesPrimaireDtos.DonneesPrimaireDto;
import com.saas.school.dto.NotesPrimaireDtos.EnregistrementNotesPrimaireRequest;
import com.saas.school.service.NotesPrimaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * =========================================================
 * 🎒 CONTROLLER NOTES PRIMAIRE (fonctionnalité isolée)
 * =========================================================
 * N'affecte pas /api/notes (NoteController), qui reste inchangé.
 */
@RestController
@RequestMapping("/api/notes-primaire")
@RequiredArgsConstructor
@CrossOrigin("*")
public class NotesPrimaireController {

    private final NotesPrimaireService notesPrimaireService;

    @GetMapping("/classe/{classeId}")
    public ResponseEntity<DonneesPrimaireDto> charger(
            @PathVariable Long classeId,
            @RequestParam Long anneeId,
            @RequestParam String mois
    ) {
        return ResponseEntity.ok(notesPrimaireService.charger(classeId, anneeId, mois));
    }

    @PostMapping
    public ResponseEntity<Void> enregistrer(@RequestBody EnregistrementNotesPrimaireRequest request) {
        notesPrimaireService.enregistrer(request);
        return ResponseEntity.ok().build();
    }
}