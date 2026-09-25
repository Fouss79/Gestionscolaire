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
     * Génère les bulletins mensuels de tous les élèves
     * actifs d'une classe pour un mois donné.
     *
     * Exemple :
     * GET /api/bulletins-mensuels/39/40/pdf?mois=SEPTEMBRE
     */
    @GetMapping("/{classeId}/{anneeId}/pdf")
    public ResponseEntity<byte[]> genererBulletinsClasse(
            @PathVariable Long classeId,
            @PathVariable Long anneeId,
            @RequestParam String mois
    ) {

        byte[] pdf = bulletinPrimairePdfService.genererClasse(
                classeId,
                anneeId,
                mois
        );

        String filename =
                "bulletins-" + mois.toLowerCase() + ".pdf";

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_PDF);

        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(filename)
                        .build()
        );

        headers.setContentLength(pdf.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}