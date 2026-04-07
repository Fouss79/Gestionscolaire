package com.saas.school.controller;


import com.saas.school.entity.Eleve;
import com.saas.school.entity.Note;
import com.saas.school.repository.EleveRepository;
import com.saas.school.repository.NoteRepository;
import com.saas.school.service.BulletinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bulletins")
@RequiredArgsConstructor
@CrossOrigin("*")
public class BulletinController {

    private final BulletinService bulletinService;
    private final NoteRepository noteRepository;
    private final EleveRepository eleveRepository;

    @GetMapping(value = "/generate", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateBulletin(
            @RequestParam Long eleveId,
            @RequestParam Long classeId,
            @RequestParam Long anneeId,
            @RequestParam String periode
    ) {

        try {

            // ================= VALIDATION =================
            if (eleveId == null || classeId == null || anneeId == null) {
                throw new RuntimeException("Paramètres invalides");
            }

            // ================= DATA =================
            Eleve eleve = eleveRepository.findById(eleveId)
                    .orElseThrow(() -> new RuntimeException("Élève introuvable"));

            List<Note> notes = noteRepository.findByClasseIdAndAnneeScolaireIdAndEleveIdAndPeriode(
                    classeId, anneeId, eleveId, periode
            );

            if (notes.isEmpty()) {
                throw new RuntimeException("Aucune note trouvée");
            }

            // ================= GENERATION PDF =================
            byte[] pdf = bulletinService.generateBulletin(notes, eleve, periode);

            // ================= HEADERS PRO =================
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                            .filename("bulletin_" + eleve.getNom() + ".pdf")
                            .build()
            );
            headers.setContentLength(pdf.length);

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}