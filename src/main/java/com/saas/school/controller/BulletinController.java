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

            // ================= VALIDATION =================
            if (inscriptionId == null || classeId == null || anneeId == null) {
                throw new RuntimeException("Paramètres invalides");
            }
            Inscription inscription = inscriptionRepository.findById(inscriptionId)
                    .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

            // ================= DATA =================
            Eleve eleve = eleveRepository.findById(inscription.getEleve().getId())
                    .orElseThrow(() -> new RuntimeException("Élève introuvable"));

            // FIX: on ne part plus uniquement des notes déjà saisies
            // (findByClasseIdAndAnneeScolaireIdAndEleveIdAndPeriode), ce qui
            // faisait disparaître du bulletin toute matière programmée pour
            // la classe mais sans note enregistrée. On part maintenant du
            // programme complet de la classe (CoefficientMatiere), filtré
            // par compatibilité sous-groupe, et on complète avec les notes
            // existantes quand elles existent.
            List<Note> notes = bulletinService.construireNotesPourBulletin(
                    inscription, classeId, anneeId, periode
            );

            if (notes.isEmpty()) {
                throw new RuntimeException("Aucune matière programmée pour cette classe");
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