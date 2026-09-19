package com.saas.school.service;

import com.saas.school.dto.ExamenRequest;
import com.saas.school.dto.ExamenResponse;
import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Classe;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Examen;
import com.saas.school.exception.ExamenBusinessException;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.ClasseRepository;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.ExamenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExamenService {

    private final ExamenRepository examenRepository;
    private final EcoleRepository ecoleRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;
    private final ClasseRepository classeRepository;

    @Transactional
    public ExamenResponse create(ExamenRequest request) {

        Ecole ecole = ecoleRepository.findById(request.getEcoleId())
                .orElseThrow(() ->
                        new ExamenBusinessException("École introuvable")
                );

        AnneeScolaire annee = anneeScolaireRepository
                .findById(request.getAnneeScolaireId())
                .orElseThrow(() ->
                        new ExamenBusinessException(
                                "Année scolaire introuvable"
                        )
                );

        if (annee.getEcole() == null
                || !annee.getEcole().getId().equals(ecole.getId())) {

            throw new ExamenBusinessException(
                    "L'année scolaire ne correspond pas à l'école indiquée"
            );
        }

        Set<Classe> classes = chargerEtVerifierClasses(
                request.getClasseIds(),
                ecole
        );

        Examen examen = new Examen();

        examen.setNom(request.getNom());
        examen.setEcole(ecole);
        examen.setAnneeScolaire(annee);
        examen.setClasses(classes);
        examen.setDateDebut(request.getDateDebut());
        examen.setDateFin(request.getDateFin());

        if (request.getStatut() != null) {
            examen.setStatut(request.getStatut());
        }

        return ExamenResponse.from(
                examenRepository.save(examen)
        );
    }

    @Transactional
    public ExamenResponse update(
            Long id,
            ExamenRequest request
    ) {

        Examen examen = getEntity(id);

        examen.setNom(request.getNom());
        examen.setDateDebut(request.getDateDebut());
        examen.setDateFin(request.getDateFin());

        if (request.getStatut() != null) {
            examen.setStatut(request.getStatut());
        }

        /*
         * L'école et l'année scolaire ne sont pas modifiables.
         *
         * Les classes peuvent être modifiées tant que cela
         * respecte l'école de l'examen.
         */
        if (request.getClasseIds() != null) {

            Set<Classe> classes = chargerEtVerifierClasses(
                    request.getClasseIds(),
                    examen.getEcole()
            );

            examen.setClasses(classes);
        }

        return ExamenResponse.from(
                examenRepository.save(examen)
        );
    }

    @Transactional
    public void delete(Long id) {

        Examen examen = getEntity(id);

        examenRepository.delete(examen);
    }

    @Transactional(readOnly = true)
    public ExamenResponse get(Long id) {

        return ExamenResponse.from(
                getEntity(id)
        );
    }

    @Transactional(readOnly = true)
    public List<ExamenResponse> listByEcole(Long ecoleId) {

        return examenRepository
                .findByEcoleId(ecoleId)
                .stream()
                .map(ExamenResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExamenResponse> listByEcoleAndAnnee(
            Long ecoleId,
            Long anneeScolaireId
    ) {

        return examenRepository
                .findByEcoleIdAndAnneeScolaireId(
                        ecoleId,
                        anneeScolaireId
                )
                .stream()
                .map(ExamenResponse::from)
                .toList();
    }

    private Set<Classe> chargerEtVerifierClasses(
            Set<Long> classeIds,
            Ecole ecole
    ) {

        if (classeIds == null || classeIds.isEmpty()) {
            throw new ExamenBusinessException(
                    "Veuillez sélectionner au moins une classe"
            );
        }

        List<Classe> classes =
                classeRepository.findAllById(classeIds);

        if (classes.size() != classeIds.size()) {
            throw new ExamenBusinessException(
                    "Une ou plusieurs classes sont introuvables"
            );
        }

        for (Classe classe : classes) {

            if (classe.getEcole() == null
                    || !classe.getEcole()
                    .getId()
                    .equals(ecole.getId())) {

                throw new ExamenBusinessException(
                        "La classe "
                                + classe.getId()
                                + " n'appartient pas à cette école"
                );
            }
        }

        return new HashSet<>(classes);
    }

    protected Examen getEntity(Long id) {

        return examenRepository
                .findById(id)
                .orElseThrow(() ->
                        new ExamenBusinessException(
                                "Examen introuvable"
                        )
                );
    }
}