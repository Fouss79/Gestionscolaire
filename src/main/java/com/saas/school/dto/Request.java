package com.saas.school.dto;

import lombok.Data;

@Data
public class Request {
    private String nom;
    private Long ecoleId;
    private Long cycleId; // optionnel, utilisé uniquement par Niveau
}