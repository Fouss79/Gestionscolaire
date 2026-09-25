package com.saas.school.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InfoDto(
        @NotNull Long inscriptionId,
        @Min(0) Integer absences,
        @Size(max = 500) String observationMaitre) {
}
