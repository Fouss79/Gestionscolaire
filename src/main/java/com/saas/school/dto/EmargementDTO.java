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
public class EmargementDTO {

    private Long id;
    private LocalDate dateHeure;
    private String jour;
    private boolean present;
    private int duree;

    // Infos issues de EmploiDuTemps
    private Long edtId;
    private String matiere;
    private String classe;
    private int heureDebut;
    private int heureFin;

    // Infos enseignant
    private Long enseignantId;
    private String enseignantNom;
    private String enseignantPrenom;
}