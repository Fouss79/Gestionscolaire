package com.saas.school.controller;

import com.saas.school.dto.RemboursementEmpruntRequestDTO;
import com.saas.school.dto.RemboursementEmpruntResponseDTO;
import com.saas.school.service.RemboursementEmpruntService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/remboursements-emprunts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RemboursementEmpruntController {

    private final RemboursementEmpruntService remboursementEmpruntService;

    /**
     * Enregistrer un remboursement
     */
    @PostMapping
    public ResponseEntity<RemboursementEmpruntResponseDTO> enregistrerRemboursement(
            @RequestBody RemboursementEmpruntRequestDTO dto
    ) {
        RemboursementEmpruntResponseDTO remboursement =
                remboursementEmpruntService.enregistrerRemboursement(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(remboursement);
    }

    /**
     * Récupérer les remboursements d'un emprunt
     */
    @GetMapping("/emprunt/{empruntId}")
    public ResponseEntity<List<RemboursementEmpruntResponseDTO>> getByEmprunt(
            @PathVariable Long empruntId
    ) {
        return ResponseEntity.ok(
                remboursementEmpruntService.getByEmprunt(empruntId)
        );
    }

    /**
     * Récupérer tous les remboursements d'une école
     */
    @GetMapping("/ecole/{ecoleId}")
    public ResponseEntity<List<RemboursementEmpruntResponseDTO>> getByEcole(
            @PathVariable Long ecoleId
    ) {
        return ResponseEntity.ok(
                remboursementEmpruntService.getByEcole(ecoleId)
        );
    }
}
