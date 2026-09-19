package com.saas.school.dto;

import com.saas.school.entity.EpreuveEnseignant;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class EpreuveEnseignantDto {

    @Data
    public static class Request {
        @NotNull
        private Long epreuveId;

        @NotNull
        private Long enseignantId;

        private EpreuveEnseignant.RoleEpreuve role; // nullable
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long epreuveId;
        private Long enseignantId;
        private String enseignantNomComplet;
        private EpreuveEnseignant.RoleEpreuve role;

        public static Response from(EpreuveEnseignant ee) {
            var ens = ee.getEnseignant();
            return Response.builder()
                    .id(ee.getId())
                    .epreuveId(ee.getEpreuve() != null ? ee.getEpreuve().getId() : null)
                    .enseignantId(ens != null ? ens.getId() : null)
                    .enseignantNomComplet(ens != null ? (ens.getPrenom() + " " + ens.getNom()) : null)
                    .role(ee.getRole())
                    .build();
        }
    }
}
