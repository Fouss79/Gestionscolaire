package com.saas.school.dto;

import lombok.Data;

@Data
public class AffectationEnseignantRequest {
    private Long enseignantId;
    private Long classeId;
    private Long coefficientMatiereId; // choisi parmi le programme du niveau/série de la classe
}