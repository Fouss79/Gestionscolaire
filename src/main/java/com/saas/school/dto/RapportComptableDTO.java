package com.saas.school.dto;

import com.saas.school.dto.OperationComptableDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RapportComptableDTO {

    // ==============================
    // RÉSUMÉ
    // ==============================

    private Double totalRecettes;

    private Double totalDepenses;

    private Double solde;

    private Integer nombreOperations;


    // ==============================
    // OPÉRATIONS
    // ==============================

    private List<OperationComptableDTO> operations;
}
