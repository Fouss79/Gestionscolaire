package com.saas.school.service;

import com.saas.school.dto.ResultatEleveDTO;
import com.saas.school.entity.Inscription;
import com.saas.school.repository.InscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResultatService {

    private final InscriptionRepository inscriptionRepository;
    private final NoteService noteService;

    /**
     * Résultats de toute une classe pour une période donnée, triés par moyenne décroissante (rang).
     */
    public List<ResultatEleveDTO> getResultatsClasse(Long classeId, Long anneeScolaireId, String periode) {

        List<Inscription> inscriptions = inscriptionRepository
                .findByClasseIdAndAnneeScolaire_Id(classeId, anneeScolaireId);

        List<ResultatEleveDTO> resultats = inscriptions.stream()
                .map(i -> mapToResultat(i, periode))
                .sorted(Comparator.comparing(
                        ResultatEleveDTO::getMoyenneGenerale,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        // Attribution du rang après tri
        for (int idx = 0; idx < resultats.size(); idx++) {
            resultats.get(idx).setRang(idx + 1);
        }

        return resultats;
    }

    /**
     * Résultats de toute une école, groupés implicitement par cycle > classe (le tri se fait côté front,
     * les données brutes suffisent car chaque ligne porte déjà cycleNom/classeNom).
     */
    public List<ResultatEleveDTO> getResultatsEcole(Long ecoleId, Long anneeScolaireId, String periode) {

        List<Inscription> inscriptions = inscriptionRepository
                .findByEcoleIdAndAnneeScolaire_Id(ecoleId, anneeScolaireId);

        return inscriptions.stream()
                .map(i -> mapToResultat(i, periode))
                .toList();
    }

    private ResultatEleveDTO mapToResultat(Inscription inscription, String periode) {

        ResultatEleveDTO dto = new ResultatEleveDTO();

        dto.setInscriptionId(inscription.getId());
        dto.setMatricule(inscription.getEleve().getMatricule());
        dto.setNom(inscription.getEleve().getNom());
        dto.setPrenom(inscription.getEleve().getPrenom());
        dto.setClasseNom(inscription.getClasse().getNomComplet());

        if (inscription.getClasse().getNiveau() != null) {
            dto.setNiveauNom(inscription.getClasse().getNiveau().getNom());
            if (inscription.getClasse().getNiveau().getCycle() != null) {
                dto.setCycleNom(inscription.getClasse().getNiveau().getCycle().getNom());
            }
        }

        // ⚠️ À adapter selon la vraie signature de ton NoteService pour la moyenne par période
        Double moyenne = noteService.calculMoyennePeriode(
                inscription.getId(),
                periode
        );

        dto.setMoyenneGenerale(moyenne);
        dto.setAppreciation(calculerAppreciation(moyenne));

        return dto;
    }

    private String calculerAppreciation(Double moyenne) {
        if (moyenne == null) return "-";
        if (moyenne >= 16) return "Très Bien";
        if (moyenne >= 14) return "Bien";
        if (moyenne >= 12) return "Assez Bien";
        if (moyenne >= 10) return "Passable";
        return "Insuffisant";
    }
}