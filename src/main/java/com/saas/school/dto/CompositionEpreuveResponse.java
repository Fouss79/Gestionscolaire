package com.saas.school.dto;

import com.saas.school.entity.CompositionEpreuve;
import com.saas.school.entity.StatutComposition;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompositionEpreuveResponse {

    private Long id;

    private Long epreuveId;
    private Long inscriptionId;
    private Long eleveId;

    private String nom;
    private String prenom;
    private String matricule;

    private Long classeId;
    private String classeNom;

    private StatutComposition statut;

    public static CompositionEpreuveResponse from(
            CompositionEpreuve composition
    ) {
        var inscription = composition.getInscription();
        var eleve = inscription != null
                ? inscription.getEleve()
                : null;
        var classe = inscription != null
                ? inscription.getClasse()
                : null;

        return CompositionEpreuveResponse.builder()
                .id(composition.getId())

                .epreuveId(
                        composition.getEpreuve() != null
                                ? composition.getEpreuve().getId()
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

                .nom(
                        eleve != null
                                ? eleve.getNom()
                                : null
                )

                .prenom(
                        eleve != null
                                ? eleve.getPrenom()
                                : null
                )

                .matricule(
                        eleve != null
                                ? eleve.getMatricule()
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

                .statut(composition.getStatut())

                .build();
    }
}