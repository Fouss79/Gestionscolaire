package com.saas.school.dto;


import lombok.Data;

@Data
public class EmploiDuTempsResponseDTO {
    private Long id;
    private Long classeId;
    private String classeNom;
    private Long matiereId;
    private String matiereNom;
    private Long enseignantId;
    private String enseignantNom;
    private String enseignantPrenom;
    private Long anneeScolaireId;
    private String jour;
    private Integer heureDebut;
    private Integer heureFin;
    private Long salleId;
    private String salleNom;
}