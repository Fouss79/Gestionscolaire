package com.saas.school.service;

import com.saas.school.dto.ExamenRequest;
import com.saas.school.dto.ExamenResponse;
import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Examen;
import com.saas.school.exception.ExamenBusinessException;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.ExamenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * NOTE : suppose l'existence de EcoleRepository et AnneeScolaireRepository
 * (standards, déjà présents dans un projet avec ces entités). Adapter les noms
 * si tes repositories existants portent un autre nom.
 */
@Service
@RequiredArgsConstructor
public class ExamenService {

    private final ExamenRepository examenRepository;
    private final EcoleRepository ecoleRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;

    @Transactional
    public ExamenResponse create(ExamenRequest request) {
        Ecole ecole = ecoleRepository.findById(request.getEcoleId())
                .orElseThrow(() -> new ExamenBusinessException("École introuvable"));

        AnneeScolaire annee = anneeScolaireRepository.findById(request.getAnneeScolaireId())
                .orElseThrow(() -> new ExamenBusinessException("Année scolaire introuvable"));

        if (annee.getEcole() == null || !annee.getEcole().getId().equals(ecole.getId())) {
            throw new ExamenBusinessException("L'année scolaire ne correspond pas à l'école indiquée");
        }

        Examen examen = new Examen();
        examen.setNom(request.getNom());
        examen.setEcole(ecole);
        examen.setAnneeScolaire(annee);
        examen.setDateDebut(request.getDateDebut());
        examen.setDateFin(request.getDateFin());

        return ExamenResponse.from(examenRepository.save(examen));
    }

    @Transactional
    public ExamenResponse update(Long id, ExamenRequest request) {
        Examen examen = getEntity(id);

        // École et année scolaire ne sont volontairement pas modifiables après création
        // pour éviter d'invalider les épreuves/affectations déjà créées.
        examen.setNom(request.getNom());
        examen.setDateDebut(request.getDateDebut());
        examen.setDateFin(request.getDateFin());

        return ExamenResponse.from(examenRepository.save(examen));
    }

    @Transactional
    public void delete(Long id) {
        Examen examen = getEntity(id);
        examenRepository.delete(examen);
    }

    public ExamenResponse get(Long id) {
        return ExamenResponse.from(getEntity(id));
    }

    public List<ExamenResponse> listByEcole(Long ecoleId) {
        return examenRepository.findByEcoleId(ecoleId).stream()
                .map(ExamenResponse::from)
                .toList();
    }

    public List<ExamenResponse> listByEcoleAndAnnee(Long ecoleId, Long anneeScolaireId) {
        return examenRepository.findByEcoleIdAndAnneeScolaireId(ecoleId, anneeScolaireId).stream()
                .map(ExamenResponse::from)
                .toList();
    }

    protected Examen getEntity(Long id) {
        return examenRepository.findById(id)
                .orElseThrow(() -> new ExamenBusinessException("Examen introuvable"));
    }
}
