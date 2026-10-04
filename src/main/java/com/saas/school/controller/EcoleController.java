package com.saas.school.controller;

import com.saas.school.entity.Ecole;
import com.saas.school.service.EcoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ecoles")
@RequiredArgsConstructor
public class EcoleController {

    private final EcoleService ecoleService;

    // =========================
    // CREER UNE ECOLE
    // =========================
    @PostMapping
    public ResponseEntity<Ecole> creer(
            @RequestBody Ecole ecole
    ) {
        return ResponseEntity.ok(
                ecoleService.creerEcole(ecole)
        );
    }

    // =========================
    // RECUPERER UNE ECOLE
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<Ecole> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ecoleService.getById(id)
        );
    }

    // =========================
    // ACTIVER / DESACTIVER
    // =========================
    @PutMapping("/toggle/{id}")
    public ResponseEntity<Ecole> toggle(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                ecoleService.toggleActive(id)
        );
    }

    // =========================
    // TOUTES LES ECOLES
    // =========================
    @GetMapping
    public ResponseEntity<List<Ecole>> getAll() {
        return ResponseEntity.ok(
                ecoleService.getAllEcoles()
        );
    }

    // =========================
    // VERIFIER LES TARIFS
    // =========================
    @GetMapping("/ecole/{ecoleId}/tarifs-configures")
    public ResponseEntity<Boolean> tarifsConfigures(
            @PathVariable Long ecoleId
    ) {
        return ResponseEntity.ok(
                ecoleService.tousLesTarifsSontConfigures(ecoleId)
        );
    }

    // =========================
    // MODIFIER UNE ECOLE
    // =========================
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Ecole> modifierEcole(

            @PathVariable Long id,

            @RequestParam(required = false)
            String nom,

            @RequestParam(required = false)
            String codeEcole,

            @RequestParam(required = false)
            String adresse,

            @RequestParam(required = false)
            String ville,

            @RequestParam(required = false)
            String pays,

            @RequestParam(required = false)
            String telephone,

            @RequestParam(required = false)
            String email,

            @RequestPart(
                    value = "logo",
                    required = false
            )
            MultipartFile logo
    ) {

        Ecole ecole =
                ecoleService.modifierEcole(
                        id,
                        nom,
                        codeEcole,
                        adresse,
                        ville,
                        pays,
                        telephone,
                        email,
                        logo
                );

        return ResponseEntity.ok(ecole);
    }
}