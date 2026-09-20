package com.saas.school.service;

import com.saas.school.dto.ExamenSalleResponse;
import com.saas.school.entity.Examen;
import com.saas.school.entity.ExamenSalle;
import com.saas.school.entity.Salle;
import com.saas.school.exception.ExamenBusinessException;
import com.saas.school.repository.ExamenRepository;
import com.saas.school.repository.ExamenSalleRepository;
import com.saas.school.repository.SalleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamenSalleService {

    private final ExamenSalleRepository examenSalleRepository;
    private final ExamenRepository examenRepository;
    private final SalleRepository salleRepository;

    @Transactional(readOnly = true)
    public List<ExamenSalleResponse> list(Long examenId) {

        getExamen(examenId);

        return examenSalleRepository.findByExamenId(examenId)
                .stream()
                .map(ExamenSalleResponse::from)
                .toList();
    }

    @Transactional
    public ExamenSalleResponse affecter(
            Long examenId,
            Long salleId
    ) {

        Examen examen = getExamen(examenId);

        Salle salle = salleRepository.findById(salleId)
                .orElseThrow(() ->
                        new ExamenBusinessException(
                                "Salle introuvable"
                        )
                );

        if (salle.getEcole() == null
                || !salle.getEcole().getId()
                .equals(examen.getEcole().getId())) {

            throw new ExamenBusinessException(
                    "Cette salle n'appartient pas à l'école de l'examen"
            );
        }

        if (!salle.isActive()) {
            throw new ExamenBusinessException(
                    "Cette salle est inactive"
            );
        }

        if (examenSalleRepository
                .existsByExamenIdAndSalleId(examenId, salleId)) {

            throw new ExamenBusinessException(
                    "Cette salle est déjà affectée à l'examen"
            );
        }

        ExamenSalle entity = new ExamenSalle();

        entity.setExamen(examen);
        entity.setSalle(salle);

        return ExamenSalleResponse.from(
                examenSalleRepository.save(entity)
        );
    }

    @Transactional
    public void retirer(
            Long examenId,
            Long salleId
    ) {

        getExamen(examenId);

        examenSalleRepository
                .deleteByExamenIdAndSalleId(
                        examenId,
                        salleId
                );
    }

    private Examen getExamen(Long id) {

        return examenRepository.findById(id)
                .orElseThrow(() ->
                        new ExamenBusinessException(
                                "Examen introuvable"
                        )
                );
    }
}