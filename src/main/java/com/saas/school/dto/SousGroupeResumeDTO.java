package com.saas.school.dto;

import lombok.Data;

@Data
public class SousGroupeResumeDTO {
    private Long id;
    private String nom;
    private String type;
    private Integer effectifActuel;
    private Integer effectifMax;
}