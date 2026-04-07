package com.saas.school.dto;

import lombok.Data;

@Data
public class AffectationDTO {
    private Long enseignantId;
    private Long classeId;
    private Long matiereId;
    private Long anneeId;

}
