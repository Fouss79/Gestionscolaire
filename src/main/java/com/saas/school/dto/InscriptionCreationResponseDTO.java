package com.saas.school.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InscriptionCreationResponseDTO {

    private Long inscriptionId;
    private Long eleveId;
}