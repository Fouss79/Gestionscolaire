package com.saas.school.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
public class EcoleResponseDTO {

    private Long id;
    private String nom;
    private String ville;
    private String pays;
    private boolean active;

    private String plan;
    private LocalDate dateFin;

}