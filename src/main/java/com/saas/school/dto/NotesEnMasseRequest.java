// DTO pour l'enregistrement groupé
package com.saas.school.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class NotesEnMasseRequest {
    private Long classeId;
    private Long matiereId;
    private Long anneeScolaireId;
    private String periode;
    private List<NoteEleveDTO> notes;
    private Long coefficientMatiereId;
    private Long sousGroupeId;


    @Data
    public static class NoteEleveDTO {

        private Long inscriptionId;

        @JsonProperty("nClass")
        private Double nClass;

        @JsonProperty("nExem")
        private Double nExem;
    }
}