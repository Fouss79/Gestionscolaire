package com.saas.school.dto;

import lombok.Data;

@Data
public class MatiereclasseDTO {
    private Long matiereId;
    private Long classeId;
    private Long anneeScolaireId;
    private int coefficient; // 🔥
    private int nombreHeures; // ex: Maths = 5h
    private Long sousGroupeId;


}
