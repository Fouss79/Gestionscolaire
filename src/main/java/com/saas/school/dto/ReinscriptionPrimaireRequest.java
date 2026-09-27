
package com.saas.school.dto;

public record ReinscriptionPrimaireRequest(
        Long ancienneInscriptionId,
        Long nouvelleClasseId
) {}