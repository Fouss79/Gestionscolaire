package com.saas.school.dto;

import java.math.BigDecimal;

public record ConduiteResultatDTO(
        Long id,
        Long inscriptionId,
        String periode,
        BigDecimal note,
        String appreciation
) {}