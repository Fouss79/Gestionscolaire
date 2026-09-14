package com.saas.school.controller;

import com.saas.school.service.FormulaireInscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inscriptions")
@RequiredArgsConstructor
@CrossOrigin("*")
public class FormulaireInscriptionController {

    private final FormulaireInscriptionService formulaireInscriptionService;

    /**
     * GET /api/inscriptions/formulaire-vierge?ecoleId=...
     * Formulaire à imprimer et faire remplir à la main, AVANT que
     * l'élève existe dans le système.
     */
    @GetMapping("/formulaire-vierge")
    public ResponseEntity<byte[]> getFormulaireVierge(@RequestParam(required = false) Long ecoleId) {

        byte[] pdf = formulaireInscriptionService.genererFormulairePdf(ecoleId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("formulaire-inscription.pdf")
                        .build()
        );

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}