package com.saas.school.dto;

import lombok.Data;

@Data
public class NoteRequest {
    private Long inscriptionId;
    private Long coefficientMatiereId; // remplace matiereId + coeff séparés
    private String periode;
    private Double nClass;
    private Double nExem;
    private Long sousGroupeId;


}