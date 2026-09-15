package com.saas.school.controller;

import com.saas.school.dto.PaiementDepenseRequestDTO;
import com.saas.school.dto.PaiementDepenseResponseDTO;
import com.saas.school.service.PaiementDepenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paiements-depense")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PaiementDepenseController {

    private final PaiementDepenseService paiementDepenseService;


    // =========================================================
    // ENREGISTRER UN PAIEMENT
    // =========================================================

    @PostMapping
    public PaiementDepenseResponseDTO enregistrer(
            @RequestBody PaiementDepenseRequestDTO dto
    ) {

        return paiementDepenseService
                .enregistrerPaiement(dto);
    }


    // =========================================================
    // PAIEMENTS D'UNE DÉPENSE
    // =========================================================

    @GetMapping("/depense/{depenseId}")
    public List<PaiementDepenseResponseDTO> getByDepense(
            @PathVariable Long depenseId
    ) {

        return paiementDepenseService
                .getByDepense(depenseId);
    }


    // =========================================================
    // PAIEMENTS D'UNE ÉCOLE POUR UNE ANNÉE
    // =========================================================

    @GetMapping("/ecole/{ecoleId}/annee/{anneeId}")
    public List<PaiementDepenseResponseDTO> getByEcoleAndAnnee(
            @PathVariable Long ecoleId,
            @PathVariable Long anneeId
    ) {

        return paiementDepenseService
                .getByEcoleAndAnnee(
                        ecoleId,
                        anneeId
                );
    }
}