package com.saas.school.dto;

import com.saas.school.entity.Salle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalleResponse {

    private Long id;
    private String nom;
    private Integer capacite;
    private boolean active;
    private Long ecoleId;

    public static SalleResponse from(Salle s) {
        return SalleResponse.builder()
                .id(s.getId())
                .nom(s.getNom())
                .capacite(s.getCapacite())
                .active(s.isActive())
                .ecoleId(s.getEcole() != null ? s.getEcole().getId() : null)
                .build();
    }
}
