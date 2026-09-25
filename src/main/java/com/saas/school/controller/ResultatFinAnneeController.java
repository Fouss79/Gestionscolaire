package com.saas.school.controller;

import com.saas.school.dto.ResultatFinAnneeDto;
import com.saas.school.service.ResultatFinAnneePdfService;
import com.saas.school.service.ResultatFinAnneePrimaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resultats-fin-annee")
@RequiredArgsConstructor
public class ResultatFinAnneeController {

    private final ResultatFinAnneePdfService pdfService;
    private final ResultatFinAnneePrimaireService resultatService;

    @GetMapping("/eleve/{inscriptionId}/pdf")
    public ResponseEntity<byte[]> genererPdfEleve(
            @PathVariable Long inscriptionId,
            @RequestParam Long anneeScolaireId) {

        ResultatFinAnneeDto dto =
                resultatService.construirePourEleve(
                        inscriptionId,
                        anneeScolaireId
                );

        byte[] pdf = pdfService.genererPdfEleve(dto);

        String nomFichier =
                "resultats-fin-annee-" + inscriptionId + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nomFichier + "\""
                )
                .body(pdf);
    }

    @GetMapping("/classe/{classeId}/pdf")
    public ResponseEntity<byte[]> genererPdfClasse(
            @PathVariable Long classeId,
            @RequestParam Long anneeScolaireId) {

        List<ResultatFinAnneeDto> dtos =
                resultatService.construirePourClasse(
                        classeId,
                        anneeScolaireId
                );

        byte[] pdf = pdfService.genererPdfClasse(dtos);

        String nomFichier =
                "resultats-fin-annee-classe-" + classeId + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nomFichier + "\""
                )
                .body(pdf);
    }
}