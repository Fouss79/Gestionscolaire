package com.saas.school.service;

import com.saas.school.dto.BesoinHeureDTO;
import com.saas.school.dto.BesoinHeureRequestDTO;
import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.BesoinHeure;
import com.saas.school.entity.Classe;
import com.saas.school.entity.Matiere;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.BesoinHeureRepository;
import com.saas.school.repository.ClasseRepository;
import com.saas.school.repository.MatiereRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BesoinHeureService {

    private final BesoinHeureRepository repo;
    private final ClasseRepository classeRepo;
    private final MatiereRepository matiereRepo;
    private final AnneeScolaireRepository anneeRepo;

    // ===================== CREATE =====================
    public BesoinHeureDTO create(BesoinHeureRequestDTO dto) {

        Classe classe = classeRepo.findById(dto.classeId)
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        Matiere matiere = matiereRepo.findById(dto.matiereId)
                .orElseThrow(() -> new RuntimeException("Matière introuvable"));

        AnneeScolaire annee = anneeRepo.findById(dto.anneeScolaireId)
                .orElseThrow(() -> new RuntimeException("Année scolaire introuvable"));

        BesoinHeure bh = new BesoinHeure();
        bh.setClasse(classe);
        bh.setMatiere(matiere);
        bh.setAnneeScolaire(annee);
        bh.setNombreHeures(dto.nombreHeures);

        return mapToDTO(repo.save(bh));
    }

    // ===================== GET ALL =====================
    public List<BesoinHeureDTO> getAll() {
        return repo.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // ===================== GET BY ID =====================
    public BesoinHeureDTO getById(Long id) {
        BesoinHeure bh = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("BesoinHeure introuvable"));

        return mapToDTO(bh);
    }

    // ===================== UPDATE =====================
    public BesoinHeureDTO update(Long id, BesoinHeureRequestDTO dto) {

        BesoinHeure bh = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("BesoinHeure introuvable"));

        Classe classe = classeRepo.findById(dto.classeId)
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        Matiere matiere = matiereRepo.findById(dto.matiereId)
                .orElseThrow(() -> new RuntimeException("Matière introuvable"));

        AnneeScolaire annee = anneeRepo.findById(dto.anneeScolaireId)
                .orElseThrow(() -> new RuntimeException("Année scolaire introuvable"));

        bh.setClasse(classe);
        bh.setMatiere(matiere);
        bh.setAnneeScolaire(annee);
        bh.setNombreHeures(dto.nombreHeures);

        return mapToDTO(repo.save(bh));
    }

    // ===================== DELETE =====================
    public void delete(Long id) {
        BesoinHeure bh = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("BesoinHeure introuvable"));

        repo.delete(bh);
    }

    // ===================== MAPPING =====================
    private BesoinHeureDTO mapToDTO(BesoinHeure bh) {
        BesoinHeureDTO dto = new BesoinHeureDTO();
        dto.id = bh.getId();
        dto.nombreHeures = bh.getNombreHeures();

        dto.classe = bh.getClasse() != null ? bh.getClasse().getNomComplet() : null;
        dto.matiere = bh.getMatiere() != null ? bh.getMatiere().getNom() : null;
        dto.anneeScolaire = bh.getAnneeScolaire() != null ? bh.getAnneeScolaire().getNom() : null;

        return dto;
    }
}
