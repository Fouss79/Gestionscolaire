package com.saas.school.dto;
import lombok.Data;

@Data
public class PersonnelResponseDTO {

    private Long id;

    private String matricule;

    private String nom;
    private String prenom;

    private String telephone;
    private String email;

    private String role;

    private Boolean actif;

}