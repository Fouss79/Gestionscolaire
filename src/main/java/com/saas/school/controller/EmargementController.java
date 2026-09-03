package com.saas.school.controller;

import com.saas.school.dto.EmargementDTO;
import com.saas.school.dto.EmargementResumeDTO;
import com.saas.school.entity.Emargement;
import com.saas.school.entity.EmploiDuTemps;
import com.saas.school.repository.EmploiDuTempsRepository;
import com.saas.school.service.EmargementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/emargement")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EmargementController {


    private final EmploiDuTempsRepository emploiRepo;
    private final EmargementService emargementService;

    // ================= EMPLOI =================
    @GetMapping("/emploi")
    public List<EmploiDuTemps> getEmploi(
            @RequestParam String date,
            @RequestParam Long anneeId
    ) {
        return emargementService.getEmploiParDate(
                LocalDate.parse(date),
                anneeId
        );
    }

    // ================= EMARGEMENT LIST =================
    @GetMapping("/jour")
    public List<Emargement> getByDate(
            @RequestParam String date
    ) {
        return emargementService.getEmargementsParDate(
                LocalDate.parse(date)
        );
    }

    // ================= EMARGER =================
    @PostMapping("/emarger/{edtId}")
    public Emargement emarger(
            @PathVariable Long edtId,
            @RequestParam String date
    ) {

        EmploiDuTemps edt = emploiRepo.findById(edtId)
                .orElseThrow(() -> new RuntimeException("EDT introuvable"));

        return emargementService.emarger(edt, LocalDate.parse(date));
    }

    @GetMapping("/enseignant/{enseignantId}")
    public ResponseEntity<List<EmargementDTO>> getEmargementsEnseignant(
            @PathVariable Long enseignantId,
            @RequestParam Long anneeId,
            @RequestParam(required = false) LocalDate debut,
            @RequestParam(required = false) LocalDate fin) {

        if (debut != null && fin != null) {
            return ResponseEntity.ok(
                    emargementService.getEmargementsParEnseignantEtPeriode(enseignantId, debut, fin, anneeId));
        }
        return ResponseEntity.ok(emargementService.getEmargementsParEnseignant(enseignantId, anneeId));
    }

    @GetMapping("/enseignant/{enseignantId}/resume")
    public ResponseEntity<EmargementResumeDTO> getResumeEnseignant(
            @PathVariable Long enseignantId,
            @RequestParam LocalDate debut,
            @RequestParam LocalDate fin,
            @RequestParam Long anneeId) {

        return ResponseEntity.ok(
                emargementService.getResumeParEnseignant(enseignantId, debut, fin, anneeId));
    }

    @GetMapping("/resume")
    public ResponseEntity<List<EmargementResumeDTO>> getResumeTousEnseignants(
            @RequestParam LocalDate debut,
            @RequestParam LocalDate fin,
            @RequestParam Long anneeId) {

        return ResponseEntity.ok(
                emargementService.getResumeTousEnseignants(debut, fin, anneeId));
    }
}