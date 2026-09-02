package com.saas.school.service;

import com.saas.school.dto.DepenseDTO;
import com.saas.school.entity.CategorieDepense;
import com.saas.school.entity.Depense;
import com.saas.school.entity.Ecole;
import com.saas.school.repository.CategorieDepenseRepository;
import com.saas.school.repository.DepenseRepository;
import com.saas.school.repository.EcoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepenseService {

    private final DepenseRepository depenseRepository;
    private final CategorieDepenseRepository categorieDepenseRepository;
    private final EcoleRepository ecoleRepository;
    private final OperationComptableService operationComptableService;

    public DepenseDTO creer(Long ecoleId, DepenseDTO dto) {

        if (dto.getLibelle() == null || dto.getLibelle().isBlank()) {
            throw new RuntimeException("Le libellé est obligatoire");
        }

        if (dto.getMontant() == null || dto.getMontant()<= 0) {
            throw new RuntimeException("Le montant doit être supérieur à zéro");
        }

        if (dto.getDateDepense() == null) {
            throw new RuntimeException("La date de dépense est obligatoire");
        }

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() ->
                        new RuntimeException("École introuvable"));

        CategorieDepense categorie =
                categorieDepenseRepository.findById(dto.getCategorieId())
                        .orElseThrow(() ->
                                new RuntimeException("Catégorie introuvable"));
        System.out.println("=================================");
        System.out.println("Categorie ID = " + categorie.getId());
        System.out.println("Categorie nom = " + categorie.getNom());
        System.out.println("Ecole catégorie = " +
                (categorie.getEcole() != null
                        ? categorie.getEcole().getId()
                        : null));
        System.out.println("=================================");

        // Vérification importante :
        // la catégorie doit appartenir à la même école
        if (categorie.getEcole() == null
                || !categorie.getEcole().getId().equals(ecoleId)) {

            throw new RuntimeException(
                    "Cette catégorie n'appartient pas à cette école"
            );
        }

        Depense depense = new Depense();

        depense.setLibelle(dto.getLibelle().trim());
        depense.setMontant(dto.getMontant());
        depense.setDateDepense(dto.getDateDepense());
        depense.setDescription(dto.getDescription());
        depense.setCategorie(categorie);
        depense.setEcole(ecole);

        Depense saved = depenseRepository.save(depense);

        operationComptableService.creerDepense(saved);

        return toDTO(saved);

    }

    public List<DepenseDTO> findByEcole(Long ecoleId) {

        return depenseRepository
                .findByEcole_IdOrderByDateDepenseDesc(ecoleId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private DepenseDTO toDTO(Depense depense) {

        DepenseDTO dto = new DepenseDTO();

        dto.setId(depense.getId());
        dto.setLibelle(depense.getLibelle());
        dto.setMontant(depense.getMontant());
        dto.setDateDepense(depense.getDateDepense());
        dto.setDescription(depense.getDescription());

        if (depense.getEcole() != null) {
            dto.setEcoleId(depense.getEcole().getId());
        }

        if (depense.getCategorie() != null) {
            dto.setCategorieId(depense.getCategorie().getId());
            dto.setCategorieNom(depense.getCategorie().getNom());
        }

        return dto;
    }
}