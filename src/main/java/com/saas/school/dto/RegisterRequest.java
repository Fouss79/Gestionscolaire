package com.saas.school.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Le nom de l'école est obligatoire")
    private String nomEcole;

    private String adresse;
    private String ville;
    private String pays;

    private String telephone;

    @Email(message = "Email invalide")
    @NotBlank(message = "Email obligatoire")
    private String email;

    @NotBlank(message = "Mot de passe obligatoire")
    private String password;
    private String image;
}
