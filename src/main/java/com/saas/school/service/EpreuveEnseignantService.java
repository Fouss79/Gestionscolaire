package com.saas.school.service;

import com.saas.school.dto.EpreuveEnseignantDto;
import com.saas.school.entity.Enseignant;
import com.saas.school.entity.EpreuveEnseignant;
import com.saas.school.entity.EpreuveExamen;
import com.saas.school.exception.ExamenBusinessException;
import com.saas.school.repository.EnseignantRepository;
import com.saas.school.repository.EpreuveEnseignantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * NOTE : suppose l'existence de EnseignantRepository (standard, déjà présent
 * dans un projet où Enseignant existe). Adapter le nom si besoin.
 */
@Service
@RequiredArgsConstructor
public class EpreuveEnseignantService {

    private final EpreuveEnseignantRepository epreuveEnseignantRepository;
    private final EnseignantRepository enseignantRepository;
    private final EpreuveExamenService epreuveExamenService;

    @Transactional
    public EpreuveEnseignantDto.Response add(EpreuveEnseignantDto.Request request) {
        EpreuveExamen epreuve = epreuveExamenService.getEntity(request.getEpreuveId());
        Enseignant enseignant = enseignantRepository.findById(request.getEnseignantId())
                .orElseThrow(() -> new ExamenBusinessException("Enseignant introuvable"));

        if (enseignant.getEcole() == null || !enseignant.getEcole().getId().equals(epreuve.getExamen().getEcole().getId())) {
            throw new ExamenBusinessException("L'enseignant n'appartient pas à l'école de l'examen");
        }

        EpreuveEnseignant ee = new EpreuveEnseignant();
        ee.setEpreuve(epreuve);
        ee.setEnseignant(enseignant);
        ee.setRole(request.getRole());

        return EpreuveEnseignantDto.Response.from(epreuveEnseignantRepository.save(ee));
    }

    @Transactional
    public void remove(Long id) {
        EpreuveEnseignant ee = epreuveEnseignantRepository.findById(id)
                .orElseThrow(() -> new ExamenBusinessException("Affectation enseignant introuvable"));
        epreuveEnseignantRepository.delete(ee);
    }

    public List<EpreuveEnseignantDto.Response> listByEpreuve(Long epreuveId) {
        return epreuveEnseignantRepository.findByEpreuveId(epreuveId).stream()
                .map(EpreuveEnseignantDto.Response::from)
                .toList();
    }
}
