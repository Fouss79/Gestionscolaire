package com.saas.school.controller;

import com.saas.school.service.ExamenDocumentPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/examens")
@RequiredArgsConstructor
public class ExamenDocumentPdfController {

    private final ExamenDocumentPdfService examenDocumentPdfService;

    // ============================================================
    // LISTES DES ÉLÈVES PAR SALLE
    // ============================================================

    @GetMapping("/{examenId}/documents/listes-salles.pdf")
    public ResponseEntity<byte[]> genererListesSalles(
            @PathVariable Long examenId
    ) {

        try {

            byte[] pdf =
                    examenDocumentPdfService
                            .genererListesSallesPdf(examenId);

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"listes-salles-examen-"
                                    + examenId
                                    + ".pdf\""
                    )
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdf.length)
                    .body(pdf);

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest().build();

        } catch (Exception e) {

            return ResponseEntity.internalServerError().build();
        }
    }

    // ============================================================
    // FEUILLE DE PRÉSENCE
    // ÉPREUVE + SALLE + CLASSE
    // ============================================================

    @GetMapping(
            "/{examenId}/epreuves/{epreuveId}/documents/feuille-presence.pdf"
    )
    public ResponseEntity<byte[]> genererFeuillePresence(
            @PathVariable Long examenId,
            @PathVariable Long epreuveId
    ) {

        try {

            byte[] pdf =
                    examenDocumentPdfService
                            .genererFeuillePresencePdf(
                                    examenId,
                                    epreuveId
                            );

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"feuille-presence-"
                                    + "examen-"
                                    + examenId
                                    + "-epreuve-"
                                    + epreuveId
                                    + ".pdf\""
                    )
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdf.length)
                    .body(pdf);

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest().build();

        } catch (Exception e) {

            return ResponseEntity.internalServerError().build();
        }
    }
}
