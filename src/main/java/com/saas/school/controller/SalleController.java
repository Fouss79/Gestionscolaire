package com.saas.school.controller;

import com.saas.school.entity.Salle;
import com.saas.school.service.SalleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salles")
@RequiredArgsConstructor
@CrossOrigin("*")
public class SalleController {

    private final SalleService salleService;

    @Data
    static class SalleRequest {
        private String nom;
        private Integer capacite;
        private Long ecoleId;
    }

    @PostMapping
    public Salle creer(@RequestBody SalleRequest request) {
        return salleService.creer(request.getNom(), request.getCapacite(), request.getEcoleId());
    }

    @GetMapping("/ecole/{ecoleId}")
    public List<Salle> getByEcole(@PathVariable Long ecoleId) {
        return salleService.getByEcole(ecoleId);
    }

    @PutMapping("/{id}")
    public Salle modifier(@PathVariable Long id, @RequestBody SalleRequest request) {
        return salleService.modifier(id, request.getNom(), request.getCapacite());
    }

    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Long id) {
        salleService.supprimer(id);
    }
}