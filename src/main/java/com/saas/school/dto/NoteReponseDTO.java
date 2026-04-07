package com.saas.school.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NoteReponseDTO {

    private Long id;

    private String eleveNom;
    private String matiereNom;
    private String classeNom;
    private String annee;

    private String periode;

    @JsonProperty("nClass")
    private Double nClass;
    @JsonProperty("nExem")
    private Double nExem;
    private Double moyenne;
    private Double coeff;

    public Double getNClass() {
        return nClass;
    }

    public void setNClass(Double nClass) {
        this.nClass = nClass;
    }

    public Double getNExem() {
        return nExem;
    }

    public void setNExem(Double nExem) {
        this.nExem = nExem;
    }
}