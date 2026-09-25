package com.saas.school.dto;

import java.math.BigDecimal;

public record LigneBulletinDto(
        String matiere,
        BigDecimal note,
        BigDecimal coef,
        String appreciation) {
}
