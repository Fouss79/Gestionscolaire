package com.saas.school.controller;

import com.saas.school.dto.RapportPaiementDTO;
import com.saas.school.service.RapportPaiementPdfService;
import com.saas.school.service.RapportPaiementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rapports/paiements")
@RequiredArgsConstructor
public class RapportPaiementController {

    private final RapportPaiementService rapportPaiementService;
    private final RapportPaiementPdfService rapportPaiementPdfService;

    // =====================================================
    // RAPPORT JSON
    // =====================================================

    @GetMapping("/inscription/{inscriptionId}")
    public ResponseEntity<RapportPaiementDTO> rapportPaiements(
            @PathVariable Long inscriptionId
    ) {

        RapportPaiementDTO rapport =
                rapportPaiementService.genererRapport(
                        inscriptionId
                );

        return ResponseEntity.ok(rapport);
    }


    // =====================================================
    // RAPPORT PDF
    // =====================================================

    @GetMapping("/inscription/{inscriptionId}/pdf")
    public ResponseEntity<byte[]> rapportPaiementsPdf(
            @PathVariable Long inscriptionId
    ) {

        // 1. Générer les données du rapport
        RapportPaiementDTO rapport =
                rapportPaiementService.genererRapport(
                        inscriptionId
                );

        // 2. Générer le PDF
        byte[] pdf =
                rapportPaiementPdfService.genererPdf(
                        rapport
                );

        // 3. Nom du fichier
        String nomFichier =
                "rapport-paiements-" + inscriptionId + ".pdf";

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_PDF
        );

        headers.setContentDisposition(
                ContentDisposition
                        .attachment()
                        .filename(nomFichier)
                        .build()
        );

        headers.setContentLength(
                pdf.length
        );

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(pdf);
    }
}