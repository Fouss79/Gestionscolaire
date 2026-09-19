package com.saas.school.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalleRepartitionApercu {
    private Long salleId;
    private String salleNom;
    private Integer capacite;
    private Integer nombreElevesPrevu;
}
