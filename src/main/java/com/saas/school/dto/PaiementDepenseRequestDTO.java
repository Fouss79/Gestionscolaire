package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PaiementDepenseRequestDTO {

    private Long depenseId;
    private Double montant;
    private String modePaiement;
    private String reference;
    private LocalDate datePaiement; // <-- ajouté
}