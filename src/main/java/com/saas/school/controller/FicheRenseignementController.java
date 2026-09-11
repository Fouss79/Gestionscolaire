package com.saas.school.controller;

import com.saas.school.service.FicheRenseignementService;
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
public class FicheRenseignementController {

    private final FicheRenseignementService ficheRenseignementService;

    @GetMapping("/{id}/fiche-renseignement")
    public ResponseEntity<byte[]> getFiche(@PathVariable Long id) {

        byte[] pdf = ficheRenseignementService.genererFichePdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("fiche-renseignement-" + id + ".pdf")
                        .build()
        );

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}