package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PaiementDepenseRequestDTO {
    private Long depenseId;
    private Long anneeId;
    private Double montant;
    private String modePaiement;
    private String reference;}