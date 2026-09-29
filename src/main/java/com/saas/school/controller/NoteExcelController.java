package com.saas.school.controller;

import com.saas.school.service.NoteExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/notes/excel")
@RequiredArgsConstructor
@CrossOrigin("*")
public class NoteExcelController {

    private final NoteExcelService noteExcelService;

    /**
     * =========================================================
     * 📤 EXPORT EXCEL
     *
     * PRIMAIRE :
     *   classeId + anneeId + mois
     *
     * SECONDAIRE :
     *   classeId + anneeId + coefficientMatiereId + periode
     * =========================================================
     */
    @GetMapping("/export")
    public ResponseEntity<?> exporter(
            @RequestParam Long classeId,
            @RequestParam Long anneeId,
            @RequestParam(required = false) Long coefficientMatiereId,
            @RequestParam(required = false) String periode,
            @RequestParam(required = false) String mois
    ) {

        try {

            byte[] fichier;

            // =====================================================
            // 🎒 PRIMAIRE
            // =====================================================
            if (mois != null && !mois.trim().isEmpty()) {

                fichier = noteExcelService.exporterPrimaire(
                        classeId,
                        anneeId,
                        mois
                );

            }

            // =====================================================
            // 🎓 SECONDAIRE
            // =====================================================
            else {

                if (coefficientMatiereId == null ||
                        periode == null ||
                        periode.trim().isEmpty()) {

                    return ResponseEntity.badRequest()
                            .body("Pour le secondaire, coefficientMatiereId et periode sont obligatoires.");
                }

                fichier = noteExcelService.exporter(
                        classeId,
                        anneeId,
                        coefficientMatiereId,
                        periode
                );
            }

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=notes.xlsx"
                    )
                    .contentType(
                            MediaType.parseMediaType(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            )
                    )
                    .body(fichier);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.internalServerError()
                    .body(
                            "Erreur lors de l'export Excel : "
                                    + e.getMessage()
                    );
        }
    }

    /**
     * =========================================================
     * 📥 IMPORT EXCEL
     * =========================================================
     */
    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> importer(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long classeId,
            @RequestParam Long anneeId,
            @RequestParam(required = false) Long coefficientMatiereId,
            @RequestParam(required = false) String periode,
            @RequestParam(required = false) String mois
    ) {

        try {

            int nombre;

            // =====================================================
            // 🎒 PRIMAIRE
            // =====================================================
            if (mois != null && !mois.trim().isEmpty()) {

                nombre = noteExcelService.importerPrimaire(
                        file,
                        classeId,
                        anneeId,
                        mois
                );

            }

            // =====================================================
            // 🎓 SECONDAIRE
            // =====================================================
            else {

                if (coefficientMatiereId == null ||
                        periode == null ||
                        periode.trim().isEmpty()) {

                    return ResponseEntity.badRequest()
                            .body(
                                    "Pour le secondaire, coefficientMatiereId et periode sont obligatoires."
                            );
                }

                nombre = noteExcelService.importer(
                        file,
                        classeId,
                        anneeId,
                        coefficientMatiereId,
                        periode
                );
            }

            return ResponseEntity.ok(
                    "Import terminé. "
                            + nombre
                            + " note(s) enregistrée(s)."
            );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.badRequest()
                    .body(
                            e.getMessage() != null
                                    ? e.getMessage()
                                    : "Erreur lors de l'import Excel."
                    );
        }
    }
}