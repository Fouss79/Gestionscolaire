package com.saas.school.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NoteRequestDTO {

    private Long eleveId;
    private Long matiereId;
    private Long classeId;
    private Long anneeScolaireId;

    private String periode;

    @JsonProperty("nClass")
    private Double nClass;

    @JsonProperty("nExem")
    private Double nExem;

    private Double coeff;
}