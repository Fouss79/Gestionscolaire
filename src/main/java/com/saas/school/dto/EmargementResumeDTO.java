package com.saas.school.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmargementResumeDTO {

    private Long enseignantId;
    private String enseignantNom;
    private String enseignantPrenom;

    private LocalDate periodeDebut;
    private LocalDate periodeFin;

    private int totalSeances;        // nombre total de séances émargées
    private int totalSeancesPrevues; // nombre total de séances prévues à l'EDT sur la période
    private int totalHeuresEmargees; // somme des durées émargées
    private double tauxPresence;     // en %
}