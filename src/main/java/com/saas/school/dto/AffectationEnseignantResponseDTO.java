package com.saas.school.dto;

import lombok.Data;

@Data
public class AffectationEnseignantResponseDTO {
    private Long id;
    private Long enseignantId;
    private String enseignantNom;
    private String enseignantPrenom;
    private Long classeId;
    private String classeNom;
    private Long coefficientMatiereId;
    private Long matiereId;
    private String matiereNom;
    private Integer coefficient;
    private Integer nombreHeuresParSemaine;
    private String anneeScolaireNom;
    private Long sousGroupeId;   // 🔥 nouveau
    private String sousGroupeNom; // 🔥 nouveau
}