package com.saas.school.service;

import com.saas.school.dto.HabilitationDto;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HabilitationService {

    private final HabilitationRepository repository;
    private final EnseignantRepository enseignantRepository;
    private final MatiereRepository matiereRepository;
    private final AnneeScolaireRepository anneeRepository;

    // ================= CREATE =================
    public Habilitation create(HabilitationDto dto) {

        Enseignant enseignant = enseignantRepository.findById(dto.getEnseignantId())
                .orElseThrow(() -> new RuntimeException("Enseignant introuvable"));

        Matiere matiere = matiereRepository.findById(dto.getMatiereId())
                .orElseThrow(() -> new RuntimeException("Matière introuvable"));

        AnneeScolaire annee = anneeRepository.findById(dto.getAnneeScolaireId())
                .orElseThrow(() -> new RuntimeException("Année introuvable"));

        // 🔥 sécurité multi-école
        if (!enseignant.getEcole().getId().equals(dto.getEcoleId())) {
            throw new RuntimeException("Accès refusé (école)");
        }

        boolean exists = repository.existsByEnseignantIdAndMatiereIdAndAnneeScolaireId(
                dto.getEnseignantId(),
                dto.getMatiereId(),
                dto.getAnneeScolaireId()
        );

        if (exists) {
            throw new RuntimeException("Cette habilitation existe déjà !");
        }

        Habilitation h = new Habilitation();
        h.setEnseignant(enseignant);
        h.setMatiere(matiere);
        h.setAnneeScolaire(annee);
        h.setEcole(enseignant.getEcole());

        return repository.save(h);
    }

    // ================= GET ENSEIGNANTS PAR MATIERE + ANNEE =================
    public List<Enseignant> getEnseignantsByMatiere(Long matiereId, Long anneeId) {

        if (matiereId == null || anneeId == null) {
            throw new RuntimeException("matiereId et anneeId sont obligatoires");
        }

        return repository.findByMatiereIdAndAnneeScolaireId(matiereId, anneeId)
                .stream()
                .map(Habilitation::getEnseignant)
                .toList();
    }

    // ================= GET PAR ECOLE =================
    public List<Habilitation> getByEcole(Long ecoleId) {
        return repository.findByEcoleId(ecoleId);
    }

    // ================= GET PAR ECOLE + ANNEE (🔥 IMPORTANT) =================
    public List<Habilitation> getByEcoleAndAnnee(Long ecoleId, Long anneeId) {

        if (ecoleId == null || anneeId == null) {
            throw new RuntimeException("ecoleId et anneeId sont obligatoires");
        }

        return repository.findByEcoleIdAndAnneeScolaireId(ecoleId, anneeId);
    }

}