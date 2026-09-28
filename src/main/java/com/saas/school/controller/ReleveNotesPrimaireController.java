
package com.saas.school.controller;

import com.saas.school.service.ReleveNotesPrimairePdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/releves-notes/primaire")
@RequiredArgsConstructor
public class ReleveNotesPrimaireController {

    private final ReleveNotesPrimairePdfService pdfService;

    @GetMapping("/classe/{classeId}/vierge/pdf")
    public ResponseEntity<byte[]> telechargerFicheVierge(
            @PathVariable Long classeId,
            @RequestParam Long anneeId,
            @RequestParam String mois
    ) {
        byte[] pdf = pdfService.generer(
                classeId,
                anneeId,
                mois
        );

        String filename =
                "fiche-vierge-primaire-"
                        + classeId
                        + "-"
                        + mois
                        + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .contentLength(pdf.length)
                .body(pdf);
    }
}