package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EleveRequest {

    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String sexe;
    private Long ecoleId;
    private Long classeId;
}