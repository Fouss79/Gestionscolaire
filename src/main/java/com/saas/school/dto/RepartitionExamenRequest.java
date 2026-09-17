package com.saas.school.dto;

import lombok.Data;

import java.util.List;

@Data
public class RepartitionExamenRequest {

    /**
     * Salles dans lesquelles les élèves seront répartis.
     */
    private List<Long> salleIds;

    /**
     * Classes dont les élèves doivent composer.
     */
    private List<Long> classeIds;

    /**
     * Mode de répartition :
     * ALPHABETIQUE
     * ALEATOIRE
     */
    private String mode;
}