package com.saas.school.controller;

import com.saas.school.entity.Emargement;
import com.saas.school.entity.EmploiDuTemps;
import com.saas.school.repository.EmargementRepository;
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
    private final EmargementRepository emargementRepo;

    // ================= 🔹 EMPLOI DU TEMPS =================
    @GetMapping("/emploi")
    public List<EmploiDuTemps> getEmploi(
            @RequestParam String jour,
            @RequestParam Long anneeId
    ) {
        return service.getEmploiParJour(jour, anneeId);
    }

    // ================= 🔹 EMARGEMENTS PAR JOUR =================
    @GetMapping("/jour")
    public List<Emargement> getByJour(
            @RequestParam String jour,
            @RequestParam String date
    ) {
        return service.getEmargementsParJour(
                jour,
                LocalDate.parse(date)
        );
    }

    // ================= 🔹 EMARGER =================
    @PostMapping("/emarger/{edtId}")
    public Emargement emarger(
            @PathVariable Long edtId,
            @RequestParam String date
    ) {
        EmploiDuTemps edt = emploiRepo.findById(edtId)
                .orElseThrow(() -> new RuntimeException("EDT introuvable"));

        return service.emarger(edt, LocalDate.parse(date));
    }

    // ================= 🔹 CHECK (OPTIONNEL MAIS PRO) =================

    // ================= 🔹 SUPPRIMER (OPTIONNEL) =================
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        emargementRepo.deleteById(id);
    }
}