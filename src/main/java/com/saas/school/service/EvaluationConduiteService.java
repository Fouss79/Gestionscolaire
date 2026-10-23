
package com.saas.school.service;

import com.saas.school.dto.ConduiteEnregistrementDTO;
import com.saas.school.dto.ConduiteSaisieDTO;
import com.saas.school.entity.EvaluationConduite;
import com.saas.school.entity.Inscription;
import com.saas.school.repository.EvaluationConduiteRepository;
import com.saas.school.repository.InscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluationConduiteService {

    private final EvaluationConduiteRepository conduiteRepository;
    private final InscriptionRepository inscriptionRepository;

    @Transactional(readOnly = true)
    public List<ConduiteSaisieDTO> lister(
            Long classeId,
            Long anneeScolaireId,
            String periode
    ) {
        if (classeId == null || anneeScolaireId == null
                || periode == null || periode.isBlank()) {
            throw new IllegalArgumentException(
                    "La classe, l'année scolaire et la période sont obligatoires"
            );
        }

        List<Inscription> inscriptions =
                inscriptionRepository.findElevesValidesPourReleve(
                        classeId, anneeScolaireId
                );

        Map<Long, EvaluationConduite> evaluations =
                conduiteRepository
                        .findByInscriptionClasseIdAndInscriptionAnneeScolaireIdAndPeriode(
                                classeId, anneeScolaireId, periode.trim()
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                e -> e.getInscription().getId(),
                                Function.identity(),
                                (a, b) -> a
                        ));

        return inscriptions.stream()
                .filter(i -> i.getEleve() != null)
                .map(i -> {
                    var eleve = i.getEleve();
                    var evaluation = evaluations.get(i.getId());

                    return new ConduiteSaisieDTO(
                            i.getId(),
                            eleve.getId(),
                            eleve.getNom(),
                            eleve.getPrenom(),
                            evaluation != null ? evaluation.getNote() : null,
                            evaluation != null
                                    ? evaluation.getAppreciation()
                                    : null
                    );
                })
                .toList();
    }

    @Transactional
    public EvaluationConduite enregistrer(ConduiteEnregistrementDTO dto) {
        if (dto == null || dto.inscriptionId() == null) {
            throw new IllegalArgumentException("L'inscription est obligatoire");
        }

        if (dto.periode() == null || dto.periode().isBlank()) {
            throw new IllegalArgumentException("La période est obligatoire");
        }

        BigDecimal note = dto.note();

        if (note == null
                || note.compareTo(BigDecimal.ZERO) < 0
                || note.compareTo(BigDecimal.valueOf(20)) > 0) {
            throw new IllegalArgumentException(
                    "La note de conduite doit être comprise entre 0 et 20"
            );
        }

        Inscription inscription = inscriptionRepository
                .findById(dto.inscriptionId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Inscription introuvable"));

        EvaluationConduite evaluation = conduiteRepository
                .findByInscriptionIdAndPeriode(
                        inscription.getId(), dto.periode().trim()
                )
                .orElseGet(EvaluationConduite::new);

        evaluation.setInscription(inscription);
        evaluation.setPeriode(dto.periode().trim());
        evaluation.setNote(note);
        evaluation.setCoefficient(1);
        evaluation.setAppreciation(
                dto.appreciation() == null
                        ? null
                        : dto.appreciation().trim()
        );

        return conduiteRepository.save(evaluation);
    }
}
