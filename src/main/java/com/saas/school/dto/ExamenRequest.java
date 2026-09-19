package com.saas.school.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ExamenRequest {

    @NotNull
    private String nom;

    @NotNull
    private Long ecoleId;

    @NotNull
    private Long anneeScolaireId;

    private LocalDate dateDebut;
    private LocalDate dateFin;
}
