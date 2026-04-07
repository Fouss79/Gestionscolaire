package com.saas.school.dto;


import lombok.Data;

@Data
public class InscriptionRequest {
    private Long eleveId;
    private Long classeId;
    private Long anneeId;

}
