package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DepenseDTO {

    private Long id;
    private Long ecoleId;

    private String libelle;
    private String description;
    private LocalDate dateDepense;

    private Double montantTotal;
    private Double montantPaye;
    private Double resteAPayer;
    private String statutPaiement; // PAYE / PARTIEL / NON_PAYE

    private Long categorieId;
    private String categorieNom;
}