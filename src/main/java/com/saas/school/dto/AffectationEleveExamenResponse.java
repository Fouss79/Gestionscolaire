package com.saas.school.dto;

import com.saas.school.entity.AffectationEleveExamen;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffectationEleveExamenResponse {

    private Long id;
    private Long examenId;
    private Long inscriptionId;
    private String eleveNomComplet;
    private String classeNom;
    private Long salleId;
    private String salleNom;
    private Integer numeroPlace;

    public static AffectationEleveExamenResponse from(AffectationEleveExamen a) {
        var inscription = a.getInscription();
        var eleve = inscription != null ? inscription.getEleve() : null;
        var classe = inscription != null ? inscription.getClasse() : null;
        var salle = a.getSalle();

        return AffectationEleveExamenResponse.builder()
                .id(a.getId())
                .examenId(a.getExamen() != null ? a.getExamen().getId() : null)
                .inscriptionId(inscription != null ? inscription.getId() : null)
                // NOTE : adapter getPrenom()/getNom() aux getters réels de l'entité Eleve
                .eleveNomComplet(eleve != null ? (eleve.getPrenom() + " " + eleve.getNom()) : null)
                .classeNom(classe != null ? classe.getNomComplet() : null)
                .salleId(salle != null ? salle.getId() : null)
                .salleNom(salle != null ? salle.getNom() : null)
                .numeroPlace(a.getNumeroPlace())
                .build();
    }
}
