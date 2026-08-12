package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PresenceResponseDTO {
    private Long id;
    private Long inscriptionId;
    private String eleveNom;
    private String elevePrenom;
    private Long edtId;
    private String matiereNom;
    private LocalDate date;
    private String statut;
    private String motif;
}