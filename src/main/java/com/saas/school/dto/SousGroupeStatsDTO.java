// SousGroupeStatsDTO.java
package com.saas.school.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SousGroupeStatsDTO {
    private Long id;
    private String nom;
    private Integer effectifActuel;
    private Integer effectifMax;
    private Long anneeScolaireId;
    private String anneeScolaireLibelle;
    private Integer effectifTotal;
}