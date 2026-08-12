package com.saas.school.dto;

import lombok.Data;

@Data
public class ClasseRequest {

    private Long niveauId;
    private Long serieId;
    private Long groupeId;
    private Long ecoleId;
    private Long salleId; // optionnel
}