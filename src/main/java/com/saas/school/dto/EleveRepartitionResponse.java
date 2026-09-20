package com.saas.school.dto;

import com.saas.school.entity.Inscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EleveRepartitionResponse {

    private Long inscriptionId;
    private Long eleveId;

    private String nom;
    private String prenom;
    private String matricule;

    private Long classeId;
    private Long anneeScolaireId;

    private String statut;

    public static EleveRepartitionResponse from(Inscription inscription) {

        var eleve = inscription.getEleve();
        var classe = inscription.getClasse();
        var annee = inscription.getAnneeScolaire();

        return EleveRepartitionResponse.builder()
                .inscriptionId(inscription.getId())
                .eleveId(eleve != null ? eleve.getId() : null)
                .nom(eleve != null ? eleve.getNom() : null)
                .prenom(eleve != null ? eleve.getPrenom() : null)
                .matricule(eleve != null ? eleve.getMatricule() : null)
                .classeId(classe != null ? classe.getId() : null)
                .anneeScolaireId(annee != null ? annee.getId() : null)
                .statut(inscription.getStatut() != null
                        ? inscription.getStatut().name()
                        : null)
                .build();
    }
}