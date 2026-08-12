package com.saas.school.controller;

import com.saas.school.dto.Request;
import com.saas.school.entity.Cycle;
import com.saas.school.service.CycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cycles")
@RequiredArgsConstructor
@CrossOrigin("*")
public class CycleController {

    private final CycleService cycleService;

    @PostMapping
    public Cycle creer(@RequestBody Request req) {
        return cycleService.creer(req.getNom(), req.getEcoleId());
    }

    @GetMapping("/ecole/{ecoleId}")
    public List<Cycle> getByEcole(@PathVariable Long ecoleId) {
        return cycleService.getByEcole(ecoleId);
    }

    @PutMapping("/{id}")
    public Cycle modifier(@PathVariable Long id, @RequestBody Request req) {
        return cycleService.modifier(id, req.getNom());
    }

    @DeleteMapping("/{id}")
    public void supprimer(@PathVariable Long id) {
        cycleService.supprimer(id);
    }
}