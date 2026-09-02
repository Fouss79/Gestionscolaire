package com.saas.school.controller;

import com.saas.school.dto.CategorieDepenseDTO;
import com.saas.school.entity.CategorieDepense;
import com.saas.school.entity.Ecole;
import com.saas.school.repository.CategorieDepenseRepository;
import com.saas.school.repository.EcoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories-depenses")
@RequiredArgsConstructor
public class CategorieDepenseController {

    private final CategorieDepenseRepository categorieDepenseRepository;
    private final EcoleRepository ecoleRepository;


    // =========================================================
    // LISTE DES CATÉGORIES DE L'ÉCOLE
    // =========================================================

    @GetMapping("/ecole/{ecoleId}")
    public ResponseEntity<List<CategorieDepenseDTO>> getCategories(
            @PathVariable Long ecoleId
    ) {

        List<CategorieDepense> categories =
                categorieDepenseRepository
                        .findByEcole_Id(ecoleId);

        List<CategorieDepenseDTO> result =
                categories.stream()
                        .map(this::toDTO)
                        .toList();

        return ResponseEntity.ok(result);
    }


    // =========================================================
    // CRÉER
    // =========================================================

    @PostMapping("/ecole/{ecoleId}")
    public ResponseEntity<CategorieDepenseDTO> creer(
            @PathVariable Long ecoleId,
            @RequestBody CategorieDepenseDTO dto
    ) {

        if (dto.getNom() == null || dto.getNom().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Ecole ecole =
                ecoleRepository.findById(ecoleId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "École introuvable"
                                )
                        );

        CategorieDepense categorie =
                new CategorieDepense();

        categorie.setNom(dto.getNom().trim());
        categorie.setEcole(ecole);

        CategorieDepense saved =
                categorieDepenseRepository.save(categorie);

        return ResponseEntity.ok(
                toDTO(saved)
        );
    }


    // =========================================================
    // MODIFIER
    // =========================================================

    @PutMapping("/{id}/ecole/{ecoleId}")
    public ResponseEntity<CategorieDepenseDTO> modifier(
            @PathVariable Long id,
            @PathVariable Long ecoleId,
            @RequestBody CategorieDepenseDTO dto
    ) {

        CategorieDepense categorie =
                categorieDepenseRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Catégorie introuvable"
                                )
                        );

        // Sécurité : la catégorie doit appartenir
        // à l'école connectée
        if (categorie.getEcole() == null
                || !categorie.getEcole()
                .getId()
                .equals(ecoleId)) {

            return ResponseEntity
                    .status(403)
                    .build();
        }

        categorie.setNom(dto.getNom().trim());

        CategorieDepense saved =
                categorieDepenseRepository.save(categorie);

        return ResponseEntity.ok(
                toDTO(saved)
        );
    }


    // =========================================================
    // SUPPRIMER
    // =========================================================

    @DeleteMapping("/{id}/ecole/{ecoleId}")
    public ResponseEntity<Void> supprimer(
            @PathVariable Long id,
            @PathVariable Long ecoleId
    ) {

        CategorieDepense categorie =
                categorieDepenseRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Catégorie introuvable"
                                )
                        );

        if (categorie.getEcole() == null
                || !categorie.getEcole()
                .getId()
                .equals(ecoleId)) {

            return ResponseEntity
                    .status(403)
                    .build();
        }

        categorieDepenseRepository.delete(categorie);

        return ResponseEntity.noContent().build();
    }


    // =========================================================
    // ENTITY → DTO
    // =========================================================

    private CategorieDepenseDTO toDTO(
            CategorieDepense categorie
    ) {

        CategorieDepenseDTO dto =
                new CategorieDepenseDTO();

        dto.setId(categorie.getId());
        dto.setNom(categorie.getNom());

        if (categorie.getEcole() != null) {
            dto.setEcoleId(
                    categorie.getEcole().getId()
            );
        }

        return dto;
    }
}
