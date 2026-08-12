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

        boolean exists = repository.existsByEnseignantIdAndMatiereId(
                dto.getEnseignantId(),
                dto.getMatiereId()
        );

        if (exists) {
            throw new RuntimeException("Cette habilitation existe déjà !");
        }

        Habilitation h = new Habilitation();
        h.setEnseignant(enseignant);
        h.setMatiere(matiere);
        h.setEcole(enseignant.getEcole());

        return repository.save(h);
    }

    // ================= GET ENSEIGNANTS PAR MATIERE + ANNEE =================
    public List<Enseignant> getEnseignantsByMatiere(Long matiereId) {

        if (matiereId == null) {
            throw new RuntimeException("matiereId est obligatoire");
        }

        return repository.findByMatiereId(matiereId)
                .stream()
                .map(Habilitation::getEnseignant)
                .filter(Enseignant::getActif)
                .toList();
    }
    // ================= GET PAR ECOLE =================
    public List<Habilitation> getByEcole(Long ecoleId) {

        return repository.findByEcoleId(ecoleId)
                .stream()
                .filter(h -> h.getEnseignant().getActif())
                .toList();
    }
    // ================= GET PAR ECOLE + ANNEE (🔥 IMPORTANT) =================


}