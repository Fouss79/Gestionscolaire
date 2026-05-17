package com.saas.school.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Paiement {

    @Id
    @GeneratedValue
    private Long id;

    private Long ecoleId;

    @Enumerated(EnumType.STRING)
    private PlanAbonnement plan;

    private int duree;

    private double montant;

    private String status; // PENDING, SUCCESS, FAILED

    private String providerRef; // ref PayDunya / Wave

    private LocalDateTime createdAt;
}