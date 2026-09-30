package com.saas.school.controller;

import com.saas.school.service.RapportEmargementVacatairePdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/emargements")
@RequiredArgsConstructor
@CrossOrigin("*")
public class RapportEmargementVacataireController {

    private final RapportEmargementVacatairePdfService pdfService;


    @GetMapping("/vacataire/{enseignantId}/pdf")
    public ResponseEntity<byte[]> genererPdfVacataire(
            @PathVariable Long enseignantId,
            @RequestParam Long anneeId,
            @RequestParam int mois,
            @RequestParam int annee
    ) throws IOException {

        byte[] pdf =
                pdfService
                        .genererPdfEnseignant(
                                enseignantId,
                                anneeId,
                                mois,
                                annee
                        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=emargements-vacataire-"
                                + enseignantId
                                + "-"
                                + annee
                                + "-"
                                + String.format("%02d", mois)
                                + ".pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}