package com.saas.school.controller;

import com.saas.school.entity.Ecole;
import com.saas.school.service.EcoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

}


