package com.saas.school.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DepenseDTO {

    private Long id;

    private String libelle;

    private Double montant;

    private LocalDate dateDepense;

    private String description;

    private Long categorieId;

    private String categorieNom;

    private Long ecoleId;
    private String reference;
    private String modePaiement;
}