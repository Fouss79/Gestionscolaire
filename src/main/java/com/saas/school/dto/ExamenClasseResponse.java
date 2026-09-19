package com.saas.school.dto;

import com.saas.school.entity.Classe;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamenClasseResponse {

    private Long id;
    private String nom;
    private Long niveauId;
    private String niveauNom;
    private Long serieId;
    private String serieNom;
    private Long groupeId;
    private String groupeNom;

    public static ExamenClasseResponse from(Classe classe) {
        return ExamenClasseResponse.builder()
                .id(classe.getId())
                .nom(classe.getNomComplet())
                .niveauId(
                        classe.getNiveau() != null
                                ? classe.getNiveau().getId()
                                : null
                )
                .niveauNom(
                        classe.getNiveau() != null
                                ? classe.getNiveau().getNom()
                                : null
                )
                .serieId(
                        classe.getSerie() != null
                                ? classe.getSerie().getId()
                                : null
                )
                .serieNom(
                        classe.getSerie() != null
                                ? classe.getSerie().getNom()
                                : null
                )
                .groupeId(
                        classe.getGroupe() != null
                                ? classe.getGroupe().getId()
                                : null
                )
                .groupeNom(
                        classe.getGroupe() != null
                                ? classe.getGroupe().getNom()
                                : null
                )
                .build();
    }
}