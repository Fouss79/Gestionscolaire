package com.saas.school.dto;

import lombok.Data;

@Data
public class EnseignantRequest {
    private String nom;
    private String prenom;
    private String telephone;
    private String specialite;
    private Long ecoleId;
}