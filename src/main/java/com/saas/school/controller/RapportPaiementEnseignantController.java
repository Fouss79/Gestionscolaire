package com.saas.school.controller;

import com.saas.school.service.RapportPaiementEnseignantPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
@CrossOrigin("*")
public class RapportPaiementEnseignantController {

    private final RapportPaiementEnseignantPdfService rapportPdfService;

    /**
     * GET /api/paiements/enseignant/{enseignantId}/rapport-pdf?anneeId=...
     */
    @GetMapping("/enseignant/{enseignantId}/rapport-pdf")
    public ResponseEntity<byte[]> getRapportPdf(
            @PathVariable Long enseignantId,
            @RequestParam Long anneeId
    ) {

        byte[] pdf = rapportPdfService.genererRapportPdf(enseignantId, anneeId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("rapport-paiements-" + enseignantId + ".pdf")
                        .build()
        );

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}