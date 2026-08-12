package com.saas.school.dto;

import lombok.Data;@Data
public class AffectationResponseDTO {

    private Long id;

    private Long enseignantId;
    private String enseignantNom;
    private String enseignantPrenom;


    private Long matiereId;
    private String matiereNom;


    private Integer coefficient;

    private Integer nombreHeuresParSemaine;


    private Long classeId;

    private Long anneeScolaireId;
}