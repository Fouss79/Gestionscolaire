package com.saas.school.dto;

import lombok.Data;

@Data
public class SousGroupeRequest {
    private String nom;
    private Long classeId;
    private Integer effectifMax;
    private String type;
    private Long anneeScolaireId; // ✅ AJOUT
}