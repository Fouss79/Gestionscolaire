package com.saas.school.controller;

import com.saas.school.dto.EleveRequest;
import com.saas.school.dto.EleveResponseDTO;
import com.saas.school.entity.Eleve;
import com.saas.school.entity.HasPermission;
import com.saas.school.service.EleveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/eleves")
@RequiredArgsConstructor
public class EleveController {

    private final EleveService eleveService;

    // 🔥 créer élève
    @PostMapping
    public ResponseEntity<Eleve> create(@RequestBody EleveRequest request) {
        return ResponseEntity.ok(eleveService.creerEleve(request));
    }
    @PostMapping(
            value = "/{id}/photo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Eleve> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                eleveService.uploadPhoto(id, file)
        );
    }
    // 📥 élèves d'une classe


    // 📥 élèves d'une école
    @HasPermission("GESTION_ELEVES")
    @GetMapping("/ecole/{ecoleId}")
    public ResponseEntity<List<Eleve>> getByEcole(@PathVariable Long ecoleId) {
        return ResponseEntity.ok(eleveService.getByEcole(ecoleId));
    }

    // 📥 élève par id
    @GetMapping("/{id}")
    public ResponseEntity<Eleve> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eleveService.getById(id));
    }

    @GetMapping("/classe/{classeId}")
    public ResponseEntity<List<EleveResponseDTO>> getByClasse(@PathVariable Long classeId) {
        return ResponseEntity.ok(eleveService.getByClasse(classeId));
    }
}
