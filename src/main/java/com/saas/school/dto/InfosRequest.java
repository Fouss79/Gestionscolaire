package com.saas.school.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record InfosRequest(
        @NotBlank String mois,
        @NotNull @Valid List<InfoDto> infos) {
}
