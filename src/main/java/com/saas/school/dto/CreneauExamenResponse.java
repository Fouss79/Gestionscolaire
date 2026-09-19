package com.saas.school.dto;

import com.saas.school.entity.CreneauExamen;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreneauExamenResponse {

    private Long id;
    private Long examenId;
    private LocalDate date;
    private LocalTime heureDebut;
    private LocalTime heureFin;

    public static CreneauExamenResponse from(CreneauExamen c) {
        return CreneauExamenResponse.builder()
                .id(c.getId())
                .examenId(c.getExamen() != null ? c.getExamen().getId() : null)
                .date(c.getDate())
                .heureDebut(c.getHeureDebut())
                .heureFin(c.getHeureFin())
                .build();
    }
}
