package com.saas.school.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreneauExamenRequest {

    @NotNull
    private Long examenId;

    @NotNull
    private LocalDate date;

    @NotNull
    private LocalTime heureDebut;

    @NotNull
    private LocalTime heureFin;
}
