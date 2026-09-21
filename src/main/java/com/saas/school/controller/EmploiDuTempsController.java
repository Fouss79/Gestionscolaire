package com.saas.school.controller;

import com.saas.school.dto.EmploiDto;
import com.saas.school.entity.EmploiDuTemps;
import com.saas.school.repository.EmploiDuTempsRepository;
import com.saas.school.service.EmargementService;
import com.saas.school.service.EmploiDuTempsPdfService;
import com.saas.school.service.EmploiDuTempsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emploi")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EmploiDuTempsController {

    private final EmploiDuTempsService emploiService;
    private final EmploiDuTempsRepository edtRepo;
    private final EmargementService emargementService;
    private final EmploiDuTempsPdfService emploiPdfService;


    // =========================================================
    // ➕ CRÉER UN CRÉNEAU
    // =========================================================

    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody EmploiDto dto
    ) {

        try {

            EmploiDuTemps edt =
                    emploiService.create(dto);

            return ResponseEntity.ok(edt);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =========================================================
    // 📅 EMPLOI DU TEMPS D'UNE CLASSE
    // =========================================================

    @GetMapping("/classe/{classeId}/{anneeId}")
    public List<EmploiDuTemps> getByClasse(
            @PathVariable Long classeId,
            @PathVariable Long anneeId
    ) {

        return emploiService.getByClasse(
                classeId,
                anneeId
        );
    }


    // =========================================================
    // 🗑️ SUPPRIMER
    // =========================================================

    @DeleteMapping("/{id}")
    public void supprimer(
            @PathVariable Long id
    ) {

        edtRepo.deleteById(id);
    }


    // =========================================================
    // ⚙️ GÉNÉRER AUTOMATIQUEMENT
    // =========================================================

    @PostMapping("/generer/{anneeId}")
    public void generer(
            @PathVariable Long anneeId
    ) {

        emploiService.generer(anneeId);
    }


    // =========================================================
    // ✏️ MODIFIER
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody EmploiDto dto
    ) {

        try {

            EmploiDuTemps edt =
                    emploiService.update(
                            id,
                            dto
                    );

            return ResponseEntity.ok(edt);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }


    // =========================================================
    // 📄 PDF EMPLOI DU TEMPS D'UNE CLASSE
    // =========================================================

    @GetMapping(
            value = "/classe/{classeId}/{anneeId}/pdf",
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> pdfClasse(
            @PathVariable Long classeId,
            @PathVariable Long anneeId
    ) {

        try {

            byte[] pdf =
                    emploiPdfService.genererPdf(
                            classeId,
                            anneeId
                    );

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_PDF
            );

            headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                            .filename(
                                    "emploi-du-temps-classe-"
                                            + classeId
                                            + ".pdf"
                            )
                            .build()
            );

            headers.setContentLength(
                    pdf.length
            );

            return new ResponseEntity<>(
                    pdf,
                    headers,
                    HttpStatus.OK
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(
                            HttpStatus.INTERNAL_SERVER_ERROR
                    )
                    .body(null);
        }
    }
}