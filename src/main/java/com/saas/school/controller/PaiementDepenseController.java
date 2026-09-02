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

    @PostMapping
    public PaiementDepenseResponseDTO enregistrer(@RequestBody PaiementDepenseRequestDTO dto) {
        return paiementDepenseService.enregistrerPaiement(dto);
    }

    @GetMapping("/depense/{depenseId}")
    public List<PaiementDepenseResponseDTO> getByDepense(@PathVariable Long depenseId) {
        return paiementDepenseService.getByDepense(depenseId);
    }

    @GetMapping("/ecole/{ecoleId}")
    public List<PaiementDepenseResponseDTO> getByEcole(@PathVariable Long ecoleId) {
        return paiementDepenseService.getByEcole(ecoleId);
    }
}