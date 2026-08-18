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

    @PostMapping
    public Ecole creer(@RequestBody Ecole ecole) {
        return ecoleService.creerEcole(ecole);
    }

    @GetMapping("/{id}")
    public Ecole getById(@PathVariable Long id) {
        return ecoleService.getById(id);
    }


    @PutMapping("/toggle/{id}")
    public Ecole toggle(@PathVariable Long id) {
        return ecoleService.toggleActive(id);
    }

    @GetMapping
    public List<Ecole> getAll() {
        return ecoleService.getAllEcoles();
    }

    @GetMapping("/ecole/{ecoleId}/tarifs-configures")
    public boolean tarifsConfigures(@PathVariable Long ecoleId) {
        return ecoleService.tousLesTarifsSontConfigures(ecoleId);
    }
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Ecole> modifierEcole(
            @PathVariable Long id,

            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String codeEcole,
            @RequestParam(required = false) String adresse,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String pays,
            @RequestParam(required = false) String telephone,
            @RequestParam(required = false) String email,

            @RequestPart(value = "logo", required = false)
            MultipartFile logo
    ) {

        Ecole ecole = ecoleService.modifierEcole(
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


