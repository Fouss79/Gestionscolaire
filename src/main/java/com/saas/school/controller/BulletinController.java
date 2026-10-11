package com.saas.school.controller;


import com.saas.school.entity.Eleve;
import com.saas.school.entity.Inscription;
import com.saas.school.entity.Note;
import com.saas.school.repository.EleveRepository;
import com.saas.school.repository.InscriptionRepository;
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
    private final InscriptionRepository inscriptionRepository;

    @GetMapping(value = "/generate", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateBulletin(
            @RequestParam Long inscriptionId,
            @RequestParam Long classeId,
            @RequestParam Long anneeId,
            @RequestParam String periode
    ) {
        try {
            Inscription inscription = inscriptionRepository.findById(inscriptionId)
                    .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

            Eleve eleve = eleveRepository.findById(inscription.getEleve().getId())
                    .orElseThrow(() -> new RuntimeException("Élève introuvable"));

            // Rang, moyenne du 1er, en-tête école : tout est géré dans le service
            byte[] pdf = bulletinService.genererBulletinPourEnvoi(
                    inscriptionId, classeId, anneeId, periode
            );

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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @GetMapping(
            value = "/generate-classe",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generateBulletinsClasse(
            @RequestParam Long classeId,
            @RequestParam Long anneeId,
            @RequestParam String periode
    ) {

        try {

            byte[] pdf =
                    bulletinService.generateBulletinsClasse(
                            classeId,
                            anneeId,
                            periode
                    );

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_PDF
            );

            headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                            .filename(
                                    "bulletins_classe_"
                                            + classeId
                                            + ".pdf"
                            )
                            .build()
            );

            headers.setContentLength(pdf.length);

            return new ResponseEntity<>(
                    pdf,
                    headers,
                    HttpStatus.OK
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}