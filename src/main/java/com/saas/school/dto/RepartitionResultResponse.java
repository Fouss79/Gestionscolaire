package com.saas.school.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepartitionResultResponse {

    private Long examenId;
    private int nombreElevesAffectes;
    private List<SalleRepartitionApercu> repartitionParSalle;
}
