package com.saas.school.dto;

import lombok.Data;

@Data
public class EmploiDto {

    private Long classeId;

    private Long matiereId;

    private Long enseignantId;

    private Long anneeId;

    private String jour;

    private int heureDebut;

    private int heureFin;

    private Long salleId;

    // 🔥 Sous-groupe concerné
    // null = cours pour toute la classe
    private Long sousGroupeId;
}