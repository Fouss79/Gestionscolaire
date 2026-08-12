package com.saas.school.dto;

import lombok.Data;

@Data
public class AffectationRequest {

    private Long enseignantId;

    private Long coefficientMatiereId;

    private Long classeId;

    private Long anneeScolaireId;
}