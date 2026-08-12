package com.saas.school.dto;

import lombok.Data;

@Data
public class NoteResponseDTO {
    private Long id;
    private Long inscriptionId;
    private String eleveNom;
    private String elevePrenom;
    private Long matiereId;
    private String matiereNom;
    private Integer coeff;
    private String periode;
    private Double nClass;
    private Double nExem;
    private Double moyenne;
    private Double points;
    private Long sousGroupeId;
    private String sousGroupeNom;


}