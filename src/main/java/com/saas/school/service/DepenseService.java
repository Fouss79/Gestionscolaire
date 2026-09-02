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

    /**
     * Crée une dépense avec son montant total. Aucun paiement n'est
     * enregistré à la création : montantPaye = 0, resteAPayer = montantTotal.
     * Chaque versement se fait ensuite via PaiementDepenseService, qui crée
     * à son tour l'opération comptable correspondante (voir
     * OperationComptableService.creerDepenseDepuisPaiement).
     */
    public DepenseDTO creer(Long ecoleId, DepenseDTO dto) {

        if (dto.getLibelle() == null || dto.getLibelle().isBlank()) {
            throw new RuntimeException("Le libellé est obligatoire");
        }

        if (dto.getMontantTotal() == null || dto.getMontantTotal() <= 0) {
            throw new RuntimeException("Le montant doit être supérieur à zéro");
        }

        if (dto.getDateDepense() == null) {
            throw new RuntimeException("La date de dépense est obligatoire");
        }

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        // La catégorie est facultative — comme pour une recette libre
        CategorieDepense categorie = null;

        if (dto.getCategorieId() != null) {

            categorie = categorieDepenseRepository.findById(dto.getCategorieId())
                    .orElseThrow(() -> new RuntimeException("Catégorie introuvable"));

            // Si une catégorie est fournie, elle doit appartenir à la même école
            if (categorie.getEcole() == null || !categorie.getEcole().getId().equals(ecoleId)) {
                throw new RuntimeException("Cette catégorie n'appartient pas à cette école");
            }
        }

        Depense depense = new Depense();

        depense.setLibelle(dto.getLibelle().trim());
        depense.setMontantTotal(dto.getMontantTotal());
        depense.setMontantPaye(0.0);
        depense.setResteAPayer(dto.getMontantTotal());
        depense.setStatutPaiement(StatutPaiement.NON_PAYE);
        depense.setDateDepense(dto.getDateDepense());
        depense.setDescription(dto.getDescription());
        depense.setCategorie(categorie);
        depense.setEcole(ecole);

        Depense saved = depenseRepository.save(depense);

        return toDTO(saved);
    }

    public List<DepenseDTO> findByEcole(Long ecoleId) {
        return depenseRepository
                .findByEcole_IdOrderByDateDepenseDesc(ecoleId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public Depense getById(Long id) {
        return depenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dépense introuvable"));
    }

    public DepenseDTO getByIdDTO(Long id) {
        return toDTO(getById(id));
    }

    DepenseDTO toDTO(Depense depense) {

        DepenseDTO dto = new DepenseDTO();

        dto.setId(depense.getId());
        dto.setLibelle(depense.getLibelle());
        dto.setMontantTotal(depense.getMontantTotal());
        dto.setMontantPaye(depense.getMontantPaye());
        dto.setResteAPayer(depense.getResteAPayer());
        dto.setStatutPaiement(depense.getStatutPaiement() != null ? depense.getStatutPaiement().name() : null);
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