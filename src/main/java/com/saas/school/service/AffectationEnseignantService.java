package com.saas.school.service;

import com.saas.school.dto.AffectationEnseignantRequest;
import com.saas.school.dto.AffectationEnseignantResponseDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AffectationEnseignantService {

    private final AffectationEnseignantRepository affectationRepository;
    private final EnseignantRepository enseignantRepository;
    private final ClasseRepository classeRepository;
    private final CoefficientMatiereRepository coefficientMatiereRepository;

    public AffectationEnseignant creer(AffectationEnseignantRequest request) {

        Enseignant enseignant = enseignantRepository.findById(request.getEnseignantId())
                .orElseThrow(() -> new RuntimeException("Enseignant introuvable"));

        Classe classe = classeRepository.findById(request.getClasseId())
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        CoefficientMatiere programme = coefficientMatiereRepository.findById(request.getCoefficientMatiereId())
                .orElseThrow(() -> new RuntimeException("Ligne de programme introuvable"));

        // 🔥 Vérifie que ce programme correspond bien au niveau/série de la classe
        boolean niveauCorrespond = programme.getNiveau().getId().equals(classe.getNiveau().getId());
        boolean serieCorrespond = programme.getSerie() == null
                || (classe.getSerie() != null && programme.getSerie().getId().equals(classe.getSerie().getId()));

        if (!niveauCorrespond || !serieCorrespond) {
            throw new RuntimeException("Cette matière ne fait pas partie du programme de cette classe");
        }

        boolean existeDeja = affectationRepository.existsByEnseignantIdAndClasseIdAndCoefficientMatiereId(
                enseignant.getId(), classe.getId(), programme.getId()
        );

        if (existeDeja) {
            throw new RuntimeException("Cet enseignant est déjà affecté à cette matière dans cette classe");
        }

        AffectationEnseignant affectation = new AffectationEnseignant();
        affectation.setEnseignant(enseignant);
        affectation.setClasse(classe);
        affectation.setCoefficientMatiere(programme);

        return affectationRepository.save(affectation);
    }

    public List<AffectationEnseignantResponseDTO> getByClasse(Long classeId, Long anneeScolaireId) {
        return affectationRepository.findByClasseIdAndCoefficientMatiere_AnneeScolaireId(classeId, anneeScolaireId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<AffectationEnseignantResponseDTO> getByEnseignant(Long enseignantId, Long anneeScolaireId) {
        return affectationRepository.findByEnseignantIdAndCoefficientMatiere_AnneeScolaireId(enseignantId, anneeScolaireId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public void supprimer(Long id) {
        if (!affectationRepository.existsById(id)) {
            throw new RuntimeException("Affectation introuvable");
        }
        affectationRepository.deleteById(id);
    }

    private AffectationEnseignantResponseDTO mapToDto(AffectationEnseignant a) {

        CoefficientMatiere programme = a.getCoefficientMatiere();

        AffectationEnseignantResponseDTO dto = new AffectationEnseignantResponseDTO();
        dto.setId(a.getId());
        dto.setEnseignantId(a.getEnseignant().getId());
        dto.setEnseignantNom(a.getEnseignant().getNom());
        dto.setEnseignantPrenom(a.getEnseignant().getPrenom());
        dto.setClasseId(a.getClasse().getId());
        dto.setClasseNom(a.getClasse().getNomComplet());
        dto.setCoefficientMatiereId(programme.getId());
        dto.setMatiereId(programme.getMatiere().getId());
        dto.setMatiereNom(programme.getMatiere().getNom());
        dto.setCoefficient(programme.getCoefficient());
        dto.setNombreHeuresParSemaine(programme.getNombreHeuresParSemaine());
        dto.setAnneeScolaireNom(programme.getAnneeScolaire().getNom());

        if (programme.getSousGroupe() != null) {
            dto.setSousGroupeId(programme.getSousGroupe().getId());
            dto.setSousGroupeNom(programme.getSousGroupe().getNom());
        }

        return dto;
    }
}