package com.saas.school.dto;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RemboursementEmpruntRequestDTO {

    private Long empruntId;

    private Double montant;

    /**
     * CASH, ORANGE_MONEY, WAVE, VIREMENT, etc.
     */
    private String modePaiement;

    /**
     * Obligatoire sauf pour CASH.
     * Pour CASH, le service génère automatiquement la référence.
     */
    private String reference;

    private LocalDate dateRemboursement; // <-- ajouté
}