package com.saas.school.dto;

import lombok.Data;

@Data
public class HabilitationDto {
    private Long enseignantId;
    private Long matiereId;
    private Long anneeScolaireId;
    private Long ecoleId;
}