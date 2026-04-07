package com.saas.school.controller;

import com.saas.school.dto.NoteReponseDTO;
import com.saas.school.dto.NoteRequestDTO;
import com.saas.school.entity.Note;
import com.saas.school.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@CrossOrigin("*")
public class NoteController {

    private final NoteService noteService;

    @GetMapping
    public List<NoteReponseDTO> getNotes(
            @RequestParam Long classeId,
            @RequestParam Long anneeScolaireId,
            @RequestParam(required = false) Long eleveId,
            @RequestParam(required = false) String periode
    ) {
        return noteService.getNotes(classeId, anneeScolaireId, eleveId, periode);
    }

    @PostMapping
    public NoteReponseDTO ajouter(@RequestBody NoteRequestDTO note) {

        System.out.println("DTO RECU 👉 " + note);
        return noteService.ajouter(note);
    }
}