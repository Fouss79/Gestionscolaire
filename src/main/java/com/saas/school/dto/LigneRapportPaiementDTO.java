package com.saas.school.dto;


import lombok.Data;

import java.util.List;

@Data
public class LigneRapportPaiementDTO {

    private Long ligneFraisId;

    private String typeFrais;

    private Integer mois;
    private Integer annee;

    private Double montantTotal;
    private Double montantPaye;
    private Double resteAPayer;

    private String statutPaiement;

    private List<PaiementDetailDTO> paiements;
}