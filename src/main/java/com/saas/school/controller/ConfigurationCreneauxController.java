package com.saas.school.controller;

import com.saas.school.dto.ConfigurationCreneauxDto;
import com.saas.school.service.ConfigurationCreneauxService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configurations-creneaux")
@RequiredArgsConstructor
public class ConfigurationCreneauxController {

    private final ConfigurationCreneauxService service;

    @GetMapping("/ecole/{ecoleId}/cycle/{cycleId}")
    public ConfigurationCreneauxDto obtenir(
            @PathVariable Long ecoleId,
            @PathVariable Long cycleId
    ) {
        return service.obtenir(ecoleId, cycleId);
    }

    @PutMapping
    public ConfigurationCreneauxDto enregistrer(
            @RequestBody ConfigurationCreneauxDto dto
    ) {
        return service.enregistrer(dto);
    }
}