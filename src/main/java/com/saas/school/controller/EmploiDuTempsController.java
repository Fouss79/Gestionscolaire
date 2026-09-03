package com.saas.school.controller;

import com.saas.school.dto.EmploiDto;
import com.saas.school.dto.EmploiDuTempsResponseDTO;
import com.saas.school.entity.Emargement;
import com.saas.school.entity.EmploiDuTemps;
import com.saas.school.repository.EmploiDuTempsRepository;
import com.saas.school.service.EmargementService;
import com.saas.school.service.EmploiDuTempsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/emploi")
@RequiredArgsConstructor
@CrossOrigin("*")
public class EmploiDuTempsController {

    private final EmploiDuTempsService emploiService;
    private final EmploiDuTempsRepository edtRepo;
    private final EmargementService emargementService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody EmploiDto dto) {
        try {
            EmploiDuTemps edt = emploiService.create(dto);
            return ResponseEntity.ok(edt);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/classe/{classeId}/{anneeId}")
    public List<EmploiDuTemps> getByClasse(
            @PathVariable Long classeId,
            @PathVariable Long anneeId
    ) {
        return emploiService.getByClasse(classeId, anneeId);
    }

    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Long id) {
        edtRepo.deleteById(id);
    }

    @PostMapping("/generer/{anneeId}")
    public void generer(@PathVariable Long anneeId) {
        emploiService.generer(anneeId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody EmploiDto dto) {
        try {
            EmploiDuTemps edt = emploiService.update(id, dto);
            return ResponseEntity.ok(edt);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}