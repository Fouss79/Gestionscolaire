package com.saas.school.service;

import com.saas.school.entity.Ecole;
import com.saas.school.entity.EpreuveExamen;
import com.saas.school.entity.EpreuveSalle;
import com.saas.school.entity.Salle;
import com.saas.school.exception.ExamenBusinessException;
import com.saas.school.repository.EpreuveExamenRepository;
import com.saas.school.repository.EpreuveSalleRepository;
import com.saas.school.repository.SalleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EpreuveSalleService {

    private final EpreuveSalleRepository epreuveSalleRepository;
    private final EpreuveExamenRepository epreuveExamenRepository;
    private final SalleRepository salleRepository;

    /**
     * Affecte une salle à une épreuve.
     *
     * Une salle peut être utilisée par plusieurs épreuves.
     * La seule interdiction est le doublon :
     * même épreuve + même salle.
     */
    @Transactional
    public EpreuveSalle affecter(Long epreuveId, Long salleId) {

        EpreuveExamen epreuve = epreuveExamenRepository.findById(epreuveId)
                .orElseThrow(() ->
                        new ExamenBusinessException("Épreuve introuvable"));

        Salle salle = salleRepository.findById(salleId)
                .orElseThrow(() ->
                        new ExamenBusinessException("Salle introuvable"));

        // Vérifier que la salle appartient à la même école
        // que l'examen.
        Ecole ecoleExamen = epreuve.getExamen().getEcole();

        if (ecoleExamen == null || salle.getEcole() == null
                || !ecoleExamen.getId().equals(salle.getEcole().getId())) {

            throw new ExamenBusinessException(
                    "La salle n'appartient pas à l'école de l'examen"
            );
        }

        // Une salle inactive ne doit pas être proposée/utilisée.
        if (!salle.isActive()) {
            throw new ExamenBusinessException(
                    "Cette salle est inactive"
            );
        }

        // Empêcher uniquement le doublon.
        if (epreuveSalleRepository.existsByEpreuveIdAndSalleId(
                epreuveId,
                salleId
        )) {
            throw new ExamenBusinessException(
                    "Cette salle est déjà affectée à cette épreuve"
            );
        }

        EpreuveSalle epreuveSalle = new EpreuveSalle();
        epreuveSalle.setEpreuve(epreuve);
        epreuveSalle.setSalle(salle);

        return epreuveSalleRepository.save(epreuveSalle);
    }

    /**
     * Liste les salles affectées à une épreuve.
     */
    @Transactional(readOnly = true)
    public List<EpreuveSalle> getByEpreuve(Long epreuveId) {

        if (!epreuveExamenRepository.existsById(epreuveId)) {
            throw new ExamenBusinessException(
                    "Épreuve introuvable"
            );
        }

        return epreuveSalleRepository.findByEpreuveId(epreuveId);
    }

    /**
     * Retire une salle d'une épreuve.
     */
    @Transactional
    public void retirer(Long epreuveId, Long salleId) {

        if (!epreuveExamenRepository.existsById(epreuveId)) {
            throw new ExamenBusinessException(
                    "Épreuve introuvable"
            );
        }

        if (!epreuveSalleRepository.existsByEpreuveIdAndSalleId(
                epreuveId,
                salleId
        )) {
            throw new ExamenBusinessException(
                    "Cette salle n'est pas affectée à cette épreuve"
            );
        }

        epreuveSalleRepository.deleteByEpreuveIdAndSalleId(
                epreuveId,
                salleId
        );
    }
}