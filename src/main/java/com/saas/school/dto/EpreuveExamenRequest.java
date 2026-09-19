package com.saas.school.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EpreuveExamenRequest {

    @NotNull
    private Long examenId;

    @NotNull
    private Long coefficientMatiereId;

    // Nullable : l'épreuve peut être créée avant d'être programmée sur un créneau.
    private Long creneauId;

    private Integer dureeMinutes;
}
