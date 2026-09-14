package com.saas.school.service;

import com.saas.school.dto.DepenseDTO;
import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.CategorieDepense;
import com.saas.school.entity.Depense;
import com.saas.school.entity.Ecole;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.CategorieDepenseRepository;
import com.saas.school.repository.DepenseRepository;
import com.saas.school.repository.EcoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepenseService {

    private final DepenseRepository depenseRepository;
    private final CategorieDepenseRepository categorieDepenseRepository;
    private final EcoleRepository ecoleRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;

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

        if (dto.getAnneeScolaireId() == null) {
            throw new RuntimeException("L'année scolaire est obligatoire");
        }

        // ---------------------------------------------------------
        // ÉCOLE
        // ---------------------------------------------------------
        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        // ---------------------------------------------------------
        // ANNÉE SCOLAIRE
        // ---------------------------------------------------------
        AnneeScolaire anneeScolaire =
                anneeScolaireRepository.findById(dto.getAnneeScolaireId())
                        .orElseThrow(() ->
                                new RuntimeException("Année scolaire introuvable"));

        // Vérifier que l'année appartient bien à l'école
        if (anneeScolaire.getEcole() == null
                || !anneeScolaire.getEcole().getId().equals(ecoleId)) {

            throw new RuntimeException(
                    "Cette année scolaire n'appartient pas à cette école"
            );
        }

        // Vérifier les dates de l'année scolaire
        if (anneeScolaire.getDateDebut() == null
                || anneeScolaire.getDateFin() == null) {

            throw new RuntimeException(
                    "Les dates de l'année scolaire sont obligatoires"
            );
        }

        LocalDate dateDepense = dto.getDateDepense();

        if (dateDepense.isBefore(anneeScolaire.getDateDebut())
                || dateDepense.isAfter(anneeScolaire.getDateFin())) {

            throw new RuntimeException(
                    "La date de la dépense (" + dateDepense +
                            ") doit être comprise entre le " +
                            anneeScolaire.getDateDebut() +
                            " et le " +
                            anneeScolaire.getDateFin()
            );
        }

        // ---------------------------------------------------------
        // CATÉGORIE
        // ---------------------------------------------------------
        CategorieDepense categorie = null;

        if (dto.getCategorieId() != null) {

            categorie = categorieDepenseRepository.findById(dto.getCategorieId())
                    .orElseThrow(() ->
                            new RuntimeException("Catégorie introuvable"));

            if (categorie.getEcole() == null
                    || !categorie.getEcole().getId().equals(ecoleId)) {

                throw new RuntimeException(
                        "Cette catégorie n'appartient pas à cette école"
                );
            }
        }

        // ---------------------------------------------------------
        // CRÉATION DE LA DÉPENSE
        // ---------------------------------------------------------
        Depense depense = new Depense();

        depense.setLibelle(dto.getLibelle().trim());
        depense.setDescription(dto.getDescription());
        depense.setMontantTotal(dto.getMontantTotal());

        depense.setMontantPaye(0.0);
        depense.setResteAPayer(dto.getMontantTotal());
        depense.setStatutPaiement(StatutPaiement.NON_PAYE);

        depense.setDateDepense(dateDepense);

        depense.setCategorie(categorie);
        depense.setEcole(ecole);

        // IMPORTANT :
        // la dépense est maintenant réellement rattachée
        // à l'année scolaire
        depense.setAnneeScolaire(anneeScolaire);

        Depense saved = depenseRepository.save(depense);

        return toDTO(saved);
    }

    // ---------------------------------------------------------
    // LISTE DES DÉPENSES D'UNE ANNÉE SCOLAIRE
    // ---------------------------------------------------------
    public List<DepenseDTO> findByEcole(Long ecoleId, Long anneeId) {

        if (ecoleId == null) {
            throw new IllegalArgumentException("L'école est obligatoire");
        }

        if (anneeId == null) {
            throw new IllegalArgumentException(
                    "L'année scolaire est obligatoire"
            );
        }

        AnneeScolaire anneeScolaire =
                anneeScolaireRepository.findById(anneeId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Année scolaire introuvable"
                                ));

        // Vérifier que l'année appartient à l'école
        if (anneeScolaire.getEcole() == null
                || !anneeScolaire.getEcole().getId().equals(ecoleId)) {

            throw new IllegalArgumentException(
                    "Cette année scolaire n'appartient pas à cette école"
            );
        }

        return depenseRepository
                .findByEcole_IdAndAnneeScolaire_IdOrderByDateDepenseDesc(
                        ecoleId,
                        anneeId
                )
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // ---------------------------------------------------------
    // GET BY ID
    // ---------------------------------------------------------
    public Depense getById(Long id) {
        return depenseRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Dépense introuvable"));
    }

    public DepenseDTO getByIdDTO(Long id) {
        return toDTO(getById(id));
    }

    // ---------------------------------------------------------
    // ENTITY -> DTO
    // ---------------------------------------------------------
    DepenseDTO toDTO(Depense depense) {

        DepenseDTO dto = new DepenseDTO();

        dto.setId(depense.getId());
        dto.setLibelle(depense.getLibelle());
        dto.setDescription(depense.getDescription());
        dto.setMontantTotal(depense.getMontantTotal());
        dto.setMontantPaye(depense.getMontantPaye());
        dto.setResteAPayer(depense.getResteAPayer());

        dto.setStatutPaiement(
                depense.getStatutPaiement() != null
                        ? depense.getStatutPaiement().name()
                        : null
        );

        dto.setDateDepense(depense.getDateDepense());

        if (depense.getEcole() != null) {
            dto.setEcoleId(depense.getEcole().getId());
        }

        if (depense.getCategorie() != null) {
            dto.setCategorieId(depense.getCategorie().getId());
            dto.setCategorieNom(depense.getCategorie().getNom());
        }

        if (depense.getAnneeScolaire() != null) {
            dto.setAnneeScolaireId(
                    depense.getAnneeScolaire().getId()
            );
        }

        return dto;
    }
}