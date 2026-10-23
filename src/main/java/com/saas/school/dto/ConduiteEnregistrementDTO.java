
package com.saas.school.dto;

import java.math.BigDecimal;

public record ConduiteEnregistrementDTO(
        Long inscriptionId,
        String periode,
        BigDecimal note,
        String appreciation
) {}
