package com.saas.school.controller;

import com.saas.school.service.BulletinPrimairePdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bulletins-mensuels")
@RequiredArgsConstructor
public class BulletinPrimaireController {

    private final BulletinPrimairePdfService bulletinPrimairePdfService;

    /**
     * ============================================================
     * PDF DE TOUS LES ÉLÈVES D'UNE CLASSE
     * ============================================================
     *
     * Exemple :
     *
     * GET /api/bulletins-mensuels/39/40/pdf?mois=SEPTEMBRE
     */
    @GetMapping("/{classeId}/{anneeId}/pdf")
    public ResponseEntity<byte[]> genererBulletinsClasse(
            @PathVariable Long classeId,
            @PathVariable Long anneeId,
            @RequestParam String mois
    ) {

        byte[] pdf =
                bulletinPrimairePdfService.genererClasse(
                        classeId,
                        anneeId,
                        mois
                );

        String filename =
                "bulletins-" +
                        mois.toLowerCase() +
                        ".pdf";

        return buildPdfResponse(pdf, filename);
    }

    /**
     * ============================================================
     * PDF INDIVIDUEL D'UN ÉLÈVE
     * ============================================================
     *
     * Exemple :
     *
     * GET
     * /api/bulletins-mensuels/39/40/eleve/125/pdf?mois=SEPTEMBRE
     */
    @GetMapping("/{classeId}/{anneeId}/eleve/{inscriptionId}/pdf")
    public ResponseEntity<byte[]> genererBulletinEleve(
            @PathVariable Long classeId,
            @PathVariable Long anneeId,
            @PathVariable Long inscriptionId,
            @RequestParam String mois
    ) {

        byte[] pdf =
                bulletinPrimairePdfService.genererEleve(
                        inscriptionId,
                        classeId,
                        anneeId,
                        mois
                );

        String filename =
                "bulletin-" +
                        inscriptionId +
                        "-" +
                        mois.toLowerCase() +
                        ".pdf";

        return buildPdfResponse(pdf, filename);
    }

    /**
     * ============================================================
     * RÉPONSE PDF COMMUNE
     * ============================================================
     */
    private ResponseEntity<byte[]> buildPdfResponse(
            byte[] pdf,
            String filename
    ) {

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_PDF
        );

        headers.setContentDisposition(
                ContentDisposition
                        .attachment()
                        .filename(filename)
                        .build()
        );

        headers.setContentLength(pdf.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}
