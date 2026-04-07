package com.saas.school.controller;

import com.saas.school.entity.Ecole;
import com.saas.school.service.EcoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}
