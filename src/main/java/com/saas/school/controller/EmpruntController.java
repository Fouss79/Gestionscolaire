package com.saas.school.controller;

import com.saas.school.dto.EmpruntDTO;
import com.saas.school.entity.Emprunt;
import com.saas.school.service.EmpruntService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emprunts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmpruntController {

    private final EmpruntService empruntService;

    /**
     * Créer un nouvel emprunt
     */
    @PostMapping("/ecole/{ecoleId}")
    public ResponseEntity<EmpruntDTO> creerEmprunt(
            @PathVariable Long ecoleId,
            @RequestBody EmpruntDTO dto
    ) {
        EmpruntDTO emprunt = empruntService.creer(ecoleId, dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(emprunt);
    }

    /**
     * Récupérer tous les emprunts d'une école
     */
    @GetMapping("/ecole/{ecoleId}")
    public ResponseEntity<List<EmpruntDTO>> getByEcole(
            @PathVariable Long ecoleId
    ) {
        return ResponseEntity.ok(
                empruntService.getByEcole(ecoleId)
        );
    }

    /**
     * Récupérer un emprunt par son ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmpruntDTO> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                empruntService.getById(id)
        );
    }
}

