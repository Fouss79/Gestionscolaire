package com.saas.school.controller;

import com.saas.school.dto.ClasseRequest;
import com.saas.school.entity.Classe;
import com.saas.school.entity.Groupe;
import com.saas.school.service.ClasseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClasseController {

    private final ClasseService classeService;

    @PostMapping
    public ResponseEntity<Classe> create(@RequestBody ClasseRequest classe) {
        return ResponseEntity.ok(classeService.creerClasse(classe));
    }
    // 📥 Tous les niveaux d'une école
    @GetMapping("/ecole/{ecoleId}")
    public ResponseEntity<List<Classe>> getByEcole(@PathVariable Long ecoleId) {
        return ResponseEntity.ok(classeService.getClasseByEcole(ecoleId));
    }

}