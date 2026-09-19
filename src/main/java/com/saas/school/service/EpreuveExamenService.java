package com.saas.school.service;

import com.saas.school.dto.EpreuveExamenRequest;
import com.saas.school.dto.EpreuveExamenResponse;
import com.saas.school.entity.CoefficientMatiere;
import com.saas.school.entity.CreneauExamen;
import com.saas.school.entity.Examen;
import com.saas.school.entity.EpreuveExamen;
import com.saas.school.exception.ExamenBusinessException;
import com.saas.school.repository.CoefficientMatiereRepository;
import com.saas.school.repository.EpreuveExamenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * NOTE : suppose l'existence de CoefficientMatiereRepository (standard, déjà présent
 * dans un projet où CoefficientMatiere existe). Adapter le nom si besoin.
 */
@Service
@RequiredArgsConstructor
public class EpreuveExamenService {

    private final EpreuveExamenRepository epreuveExamenRepository;
    private final CoefficientMatiereRepository coefficientMatiereRepository;
    private final ExamenService examenService;
    private final CreneauExamenService creneauExamenService;

    @Transactional
    public EpreuveExamenResponse add(EpreuveExamenRequest request) {
        Examen examen = examenService.getEntity(request.getExamenId());
        CoefficientMatiere cm = coefficientMatiereRepository.findById(request.getCoefficientMatiereId())
                .orElseThrow(() -> new ExamenBusinessException("CoefficientMatiere introuvable"));

        validateCoherenceEcoleAnnee(examen, cm);

        CreneauExamen creneau = null;
        if (request.getCreneauId() != null) {
            creneau = creneauExamenService.getEntity(request.getCreneauId());
            validateCreneauAppartientExamen(examen, creneau);
            validateAucunConflitCreneau(examen.getId(), cm, creneau, null);
        }

        EpreuveExamen epreuve = new EpreuveExamen();
        epreuve.setExamen(examen);
        epreuve.setCoefficientMatiere(cm);
        epreuve.setCreneau(creneau);
        epreuve.setDureeMinutes(request.getDureeMinutes());

        return EpreuveExamenResponse.from(epreuveExamenRepository.save(epreuve));
    }

    @Transactional
    public EpreuveExamenResponse update(Long id, EpreuveExamenRequest request) {
        EpreuveExamen epreuve = getEntity(id);
        Examen examen = epreuve.getExamen();

        if (!examen.getId().equals(request.getExamenId())) {
            throw new ExamenBusinessException("Une épreuve ne peut pas changer d'examen");
        }

        CoefficientMatiere cm = coefficientMatiereRepository.findById(request.getCoefficientMatiereId())
                .orElseThrow(() -> new ExamenBusinessException("CoefficientMatiere introuvable"));
        validateCoherenceEcoleAnnee(examen, cm);

        CreneauExamen creneau = null;
        if (request.getCreneauId() != null) {
            creneau = creneauExamenService.getEntity(request.getCreneauId());
            validateCreneauAppartientExamen(examen, creneau);
            validateAucunConflitCreneau(examen.getId(), cm, creneau, epreuve.getId());
        }

        epreuve.setCoefficientMatiere(cm);
        epreuve.setCreneau(creneau);
        epreuve.setDureeMinutes(request.getDureeMinutes());

        return EpreuveExamenResponse.from(epreuveExamenRepository.save(epreuve));
    }

    @Transactional
    public void delete(Long id) {
        epreuveExamenRepository.delete(getEntity(id));
    }

    public List<EpreuveExamenResponse> listByExamen(Long examenId) {
        return epreuveExamenRepository.findByExamenId(examenId).stream()
                .map(EpreuveExamenResponse::from)
                .toList();
    }

    protected EpreuveExamen getEntity(Long id) {
        return epreuveExamenRepository.findById(id)
                .orElseThrow(() -> new ExamenBusinessException("Épreuve introuvable"));
    }

    private void validateCoherenceEcoleAnnee(Examen examen, CoefficientMatiere cm) {
        if (cm.getEcole() == null || !cm.getEcole().getId().equals(examen.getEcole().getId())) {
            throw new ExamenBusinessException("Le programme (CoefficientMatiere) n'appartient pas à l'école de l'examen");
        }
        if (cm.getAnneeScolaire() == null || !cm.getAnneeScolaire().getId().equals(examen.getAnneeScolaire().getId())) {
            throw new ExamenBusinessException("Le programme (CoefficientMatiere) n'appartient pas à l'année scolaire de l'examen");
        }
    }

    private void validateCreneauAppartientExamen(Examen examen, CreneauExamen creneau) {
        if (!creneau.getExamen().getId().equals(examen.getId())) {
            throw new ExamenBusinessException("Le créneau n'appartient pas à cet examen");
        }
    }

    /**
     * Empêche de programmer deux épreuves différentes sur le même créneau pour
     * le même (niveau, série) — un élève ne peut pas composer deux matières en même temps.
     * excludeEpreuveId permet d'ignorer l'épreuve elle-même lors d'un update.
     */
    private void validateAucunConflitCreneau(Long examenId, CoefficientMatiere cm, CreneauExamen creneau, Long excludeEpreuveId) {
        List<EpreuveExamen> autresSurMemeCreneau = epreuveExamenRepository.findByCreneauId(creneau.getId());

        Long niveauId = cm.getNiveau() != null ? cm.getNiveau().getId() : null;
        Long serieId = cm.getSerie() != null ? cm.getSerie().getId() : null;

        boolean conflit = autresSurMemeCreneau.stream()
                .filter(e -> !e.getId().equals(excludeEpreuveId))
                .anyMatch(e -> {
                    var autreCm = e.getCoefficientMatiere();
                    Long autreNiveauId = autreCm.getNiveau() != null ? autreCm.getNiveau().getId() : null;
                    Long autreSerieId = autreCm.getSerie() != null ? autreCm.getSerie().getId() : null;

                    boolean memeNiveau = Objects.equals(niveauId, autreNiveauId);
                    // Conflit si les séries se recoupent : l'une des deux est null (= toutes séries) ou identiques
                    boolean serieChevauche = serieId == null || autreSerieId == null || Objects.equals(serieId, autreSerieId);

                    return memeNiveau && serieChevauche;
                });

        if (conflit) {
            throw new ExamenBusinessException(
                    "Conflit : une autre épreuve concernant le même niveau/série est déjà programmée sur ce créneau");
        }
    }
}
