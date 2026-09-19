package com.saas.school.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class SalleRequest {

    @NotNull
    private String nom;

    @NotNull
    @Positive
    private Integer capacite;

    @NotNull
    private Long ecoleId;

    private Boolean active; // défaut true si non fourni (géré côté service)
}
