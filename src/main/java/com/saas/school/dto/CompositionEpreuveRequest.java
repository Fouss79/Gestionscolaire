package com.saas.school.dto;

import com.saas.school.entity.StatutComposition;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompositionEpreuveRequest {

    @NotNull
    private StatutComposition statut;
}