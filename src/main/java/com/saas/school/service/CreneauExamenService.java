package com.saas.school.service;

import com.saas.school.dto.CreneauExamenRequest;
import com.saas.school.dto.CreneauExamenResponse;
import com.saas.school.entity.CreneauExamen;
import com.saas.school.entity.Examen;
import com.saas.school.exception.ExamenBusinessException;
import com.saas.school.repository.CreneauExamenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CreneauExamenService {

    private final CreneauExamenRepository creneauExamenRepository;
    private final ExamenService examenService;

    @Transactional
    public CreneauExamenResponse create(CreneauExamenRequest request) {
        Examen examen = examenService.getEntity(request.getExamenId());

        if (!request.getHeureDebut().isBefore(request.getHeureFin())) {
            throw new ExamenBusinessException("heureDebut doit être avant heureFin");
        }

        CreneauExamen creneau = new CreneauExamen();
        creneau.setExamen(examen);
        creneau.setDate(request.getDate());
        creneau.setHeureDebut(request.getHeureDebut());
        creneau.setHeureFin(request.getHeureFin());

        return CreneauExamenResponse.from(creneauExamenRepository.save(creneau));
    }

    @Transactional
    public CreneauExamenResponse update(Long id, CreneauExamenRequest request) {
        CreneauExamen creneau = getEntity(id);

        if (!creneau.getExamen().getId().equals(request.getExamenId())) {
            throw new ExamenBusinessException("Un créneau ne peut pas changer d'examen");
        }
        if (!request.getHeureDebut().isBefore(request.getHeureFin())) {
            throw new ExamenBusinessException("heureDebut doit être avant heureFin");
        }

        creneau.setDate(request.getDate());
        creneau.setHeureDebut(request.getHeureDebut());
        creneau.setHeureFin(request.getHeureFin());

        return CreneauExamenResponse.from(creneauExamenRepository.save(creneau));
    }

    @Transactional
    public void delete(Long id) {
        creneauExamenRepository.delete(getEntity(id));
    }

    public List<CreneauExamenResponse> listByExamen(Long examenId) {
        return creneauExamenRepository.findByExamenIdOrderByDateAscHeureDebutAsc(examenId).stream()
                .map(CreneauExamenResponse::from)
                .toList();
    }

    protected CreneauExamen getEntity(Long id) {
        return creneauExamenRepository.findById(id)
                .orElseThrow(() -> new ExamenBusinessException("Créneau introuvable"));
    }
}
