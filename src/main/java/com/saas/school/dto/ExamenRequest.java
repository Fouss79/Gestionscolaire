package com.saas.school.dto;

import com.saas.school.entity.Examen;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class ExamenRequest {

    @NotBlank
    private String nom;

    @NotNull
    private Long ecoleId;

    @NotNull
    private Long anneeScolaireId;

    @NotEmpty
    private Set<Long> classeIds = new HashSet<>();

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private Examen.StatutExamen statut;
}