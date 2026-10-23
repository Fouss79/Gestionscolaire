
package com.saas.school.dto;

import java.math.BigDecimal;

public record ConduiteSaisieDTO(
        Long inscriptionId,
        Long eleveId,
        String nom,
        String prenom,
        BigDecimal note,
        String appreciation
) {}
