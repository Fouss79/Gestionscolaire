package com.saas.school.controller;

import com.saas.school.entity.Emargement;
import com.saas.school.entity.EmploiDuTemps;
import com.saas.school.repository.EmploiDuTempsRepository;
import com.saas.school.service.EmargementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/emargement")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EmargementController {

    private final EmargementService service;
    private final EmploiDuTempsRepository emploiRepo;

    // ================= EMPLOI =================
    @GetMapping("/emploi")
    public List<EmploiDuTemps> getEmploi(
            @RequestParam String date,
            @RequestParam Long anneeId
    ) {
        return service.getEmploiParDate(
                LocalDate.parse(date),
                anneeId
        );
    }

    // ================= EMARGEMENT LIST =================
    @GetMapping("/jour")
    public List<Emargement> getByDate(
            @RequestParam String date
    ) {
        return service.getEmargementsParDate(
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

        return service.emarger(edt, LocalDate.parse(date));
    }
}