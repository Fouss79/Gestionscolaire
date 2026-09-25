package com.saas.school.dto;

import java.util.List;

public record ConfigurationCreneauxDto(
        Long ecoleId,
        Long cycleId,
        List<CreneauDto> creneaux
) {
    public record CreneauDto(
            String jour,
            int heureDebut,
            int heureFin,
            int ordre
    ) {}
}