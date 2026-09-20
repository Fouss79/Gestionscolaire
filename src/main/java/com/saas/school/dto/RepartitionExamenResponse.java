package com.saas.school.dto;

import com.saas.school.entity.RepartitionExamen;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepartitionExamenResponse {

    private Long id;

    private Long examenId;

    private Long inscriptionId;

    private Long eleveId;

    private String eleveNom;

    private String elevePrenom;

    private Long classeId;

    private String classeNom;

    private Long salleId;

    private String salleNom;

    private Integer salleCapacite;

    public static RepartitionExamenResponse from(
            RepartitionExamen entity
    ) {

        var inscription = entity.getInscription();
        var eleve = inscription != null
                ? inscription.getEleve()
                : null;

        var classe = inscription != null
                ? inscription.getClasse()
                : null;

        var salle = entity.getSalle();

        return RepartitionExamenResponse.builder()
                .id(entity.getId())
                .examenId(
                        entity.getExamen() != null
                                ? entity.getExamen().getId()
                                : null
                )
                .inscriptionId(
                        inscription != null
                                ? inscription.getId()
                                : null
                )
                .eleveId(
                        eleve != null
                                ? eleve.getId()
                                : null
                )
                .eleveNom(
                        eleve != null
                                ? eleve.getNom()
                                : null
                )
                .elevePrenom(
                        eleve != null
                                ? eleve.getPrenom()
                                : null
                )
                .classeId(
                        classe != null
                                ? classe.getId()
                                : null
                )
                .classeNom(
                        classe != null
                                ? classe.getNomComplet()
                                : null
                )
                .salleId(
                        salle != null
                                ? salle.getId()
                                : null
                )
                .salleNom(
                        salle != null
                                ? salle.getNom()
                                : null
                )
                .salleCapacite(
                        salle != null
                                ? salle.getCapacite()
                                : null
                )
                .build();
    }
}