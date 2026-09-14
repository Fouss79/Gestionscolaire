package com.saas.school.controller;

import com.saas.school.service.ReleveNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/releves-notes")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ReleveNoteController {

    private final ReleveNoteService releveNoteService;

    @GetMapping("/affectation/{affectationId}/pdf")
    public ResponseEntity<byte[]> genererRelevePdf(
            @PathVariable Long affectationId
    ) {

        byte[] pdf =
                releveNoteService.genererRelevePdf(affectationId);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_PDF
        );

        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(
                                "releve-notes-"
                                        + affectationId
                                        + ".pdf"
                        )
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}