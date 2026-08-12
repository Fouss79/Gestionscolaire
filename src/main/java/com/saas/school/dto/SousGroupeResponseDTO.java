package com.saas.school.dto;

import lombok.Data;

import java.util.List;

@Data
public class SousGroupeResponseDTO {
    private Long id;
    private String nom;
    private String type;
    private Long classeId;
    private String classeNom;
    private Integer effectifMax;
    private Integer effectifActuel;
    private List<Long> eleveIds;
    private Long anneeScolaireId;
    private String anneeScolaireNom;
}