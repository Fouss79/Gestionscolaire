package com.saas.school.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AnneeRequestDTO {

    private String nom;

    private LocalDate debut;

    private LocalDate fin;

    private Long ecoleId;


}