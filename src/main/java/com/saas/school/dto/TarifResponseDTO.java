package com.saas.school.dto;

import lombok.Data;

@Data
public class TarifResponseDTO {
    private Long id;
    private Double montant;
    private Long niveauId;
    private String niveauNom;
    private Long anneeScolaireId;
    private String anneeNom;
    private Long typeFraisId;
    private String typeFraisCode;
    private String typeFraisLibelle;
    private Long ecoleId;
}