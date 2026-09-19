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
public class RepartitionPreviewResponse {

    private Long examenId;
    private int nombreEleves;
    private int capaciteTotale;
    private int nombreSalles;
    private boolean capaciteSuffisante;
    private List<SalleRepartitionApercu> repartitionPrevue;
}
