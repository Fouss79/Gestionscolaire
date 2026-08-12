package com.saas.school.controller;


import com.saas.school.dto.Request;
import com.saas.school.entity.Groupe;
import com.saas.school.entity.Niveau;
import com.saas.school.service.GroupeService;
import com.saas.school.service.NiveauService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groupes")
@RequiredArgsConstructor
public class GroupeController {

    private final GroupeService groupeService;

    // 🔥 Créer niveau
    @PostMapping
    public ResponseEntity<Groupe> create(@RequestBody Request request) {
        Groupe groupe = groupeService.creerGroupe(
                request.getNom(),
                request.getEcoleId()
        );
        return ResponseEntity.ok(groupe);
    }

    // 📥 Tous les niveaux d'une école
    @GetMapping("/ecole/{ecoleId}")
    public ResponseEntity<List<Groupe>> getByEcole(@PathVariable Long ecoleId) {
        return ResponseEntity.ok(groupeService.getGroupesByEcole(ecoleId));
    }

    // 📥 Niveau par ID
    @GetMapping("/{id}")
    public ResponseEntity<Groupe> getById(@PathVariable Long id) {
        return ResponseEntity.ok(groupeService.getById(id));
    }

    // ❌ Supprimer
    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        groupeService.delete(id);
        return ResponseEntity.ok("Groupe supprimé");

    }

    @PutMapping("/{id}")
    public Groupe modifier(@PathVariable Long id, @RequestBody Request req) {
        return groupeService.modifier(id, req.getNom());
    }

}