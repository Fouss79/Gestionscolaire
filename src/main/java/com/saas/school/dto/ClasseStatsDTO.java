// ClasseStatsDTO.java
package com.saas.school.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClasseStatsDTO {
    private Long id;
    private String nomComplet;
    private Long classeId;
    private Long niveauId;
    private String niveauNom;
    private Long serieId;
    private String serieNom;
    private String groupeNom;
    private Integer nbElevesInscrits;
    private Integer nbElevesValides;
    private List<SousGroupeStatsDTO> sousGroupes;
    private String cycleNom;
}