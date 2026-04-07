package com.saas.school.controller;

import com.saas.school.dto.Request;
import com.saas.school.entity.Niveau;
import com.saas.school.service.NiveauService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/niveaux")
@RequiredArgsConstructor
@CrossOrigin
public class NiveauController {

    private final NiveauService niveauService;

    @GetMapping("/ecole/{ecoleId}")
    public List<Niveau> getByEcole(@PathVariable Long ecoleId) {
        return niveauService.getByEcole(ecoleId);
    }

    @PostMapping
    public Niveau create(@RequestBody Request req) {
        return niveauService.create(req.getNom(), req.getEcoleId());
    }
}