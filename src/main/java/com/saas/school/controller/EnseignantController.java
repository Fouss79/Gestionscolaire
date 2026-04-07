package com.saas.school.controller;

import com.saas.school.dto.EnseignantRequest;
import com.saas.school.entity.Enseignant;
import com.saas.school.service.EnseignantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/enseignants")
@RequiredArgsConstructor
@CrossOrigin
public class EnseignantController {

    private final EnseignantService enseignantService;

    @PostMapping
    public Enseignant create(@RequestBody EnseignantRequest request) {
        return enseignantService.create(
                request.getNom(),
                request.getPrenom(),
                request.getTelephone(),
                request.getEcoleId(),
                request.getSpecialite()
        );
    }

    @GetMapping("/ecole/{ecoleId}")
    public List<Enseignant> getByEcole(@PathVariable Long ecoleId) {
        return enseignantService.getByEcole(ecoleId);
    }
}