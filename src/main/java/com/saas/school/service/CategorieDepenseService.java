package com.saas.school.service;

import com.saas.school.entity.CategorieDepense;
import com.saas.school.entity.Ecole;
import com.saas.school.repository.CategorieDepenseRepository;
import com.saas.school.repository.EcoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategorieDepenseService {

    private final CategorieDepenseRepository categorieDepenseRepository;
    private final EcoleRepository ecoleRepository;


    // =========================================================
    // LISTE DES CATÉGORIES D'UNE ÉCOLE
    // =========================================================

    @Transactional(readOnly = true)
    public List<CategorieDepense> getByEcole(Long ecoleId) {

        verifierEcole(ecoleId);

        return categorieDepenseRepository
                .findByEcole_IdOrderByNomAsc(ecoleId);
    }


    // =========================================================
    // CRÉER
    // =========================================================

    @Transactional
    public CategorieDepense creer(
            Long ecoleId,
            String nom
    ) {

        verifierEcole(ecoleId);

        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException(
                    "Le nom de la catégorie est obligatoire"
            );
        }

        nom = nom.trim();

        if (categorieDepenseRepository
                .existsByEcole_IdAndNomIgnoreCase(
                        ecoleId,
                        nom
                )) {

            throw new IllegalArgumentException(
                    "Cette catégorie existe déjà"
            );
        }

        Ecole ecole = ecoleRepository
                .findById(ecoleId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "École introuvable"
                        )
                );

        CategorieDepense categorie =
                new CategorieDepense();

        categorie.setNom(nom);
        categorie.setEcole(ecole);

        return categorieDepenseRepository.save(
                categorie
        );
    }


    // =========================================================
    // MODIFIER
    // =========================================================

    @Transactional
    public CategorieDepense modifier(
            Long id,
            Long ecoleId,
            String nom
    ) {

        verifierEcole(ecoleId);

        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException(
                    "Le nom de la catégorie est obligatoire"
            );
        }

        nom = nom.trim();

        CategorieDepense categorie =
                categorieDepenseRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Catégorie introuvable"
                                )
                        );

        // Sécurité : la catégorie doit appartenir
        // à l'école connectée
        if (categorie.getEcole() == null
                || !categorie.getEcole()
                .getId()
                .equals(ecoleId)) {

            throw new RuntimeException(
                    "Cette catégorie n'appartient pas à cette école"
            );
        }

        if (!categorie.getNom().equalsIgnoreCase(nom)
                && categorieDepenseRepository
                .existsByEcole_IdAndNomIgnoreCase(
                        ecoleId,
                        nom
                )) {

            throw new IllegalArgumentException(
                    "Cette catégorie existe déjà"
            );
        }

        categorie.setNom(nom);

        return categorieDepenseRepository.save(
                categorie
        );
    }


    // =========================================================
    // SUPPRIMER
    // =========================================================

    @Transactional
    public void supprimer(
            Long id,
            Long ecoleId
    ) {

        verifierEcole(ecoleId);

        CategorieDepense categorie =
                categorieDepenseRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Catégorie introuvable"
                                )
                        );

        if (categorie.getEcole() == null
                || !categorie.getEcole()
                .getId()
                .equals(ecoleId)) {

            throw new RuntimeException(
                    "Cette catégorie n'appartient pas à cette école"
            );
        }

        categorieDepenseRepository.delete(
                categorie
        );
    }


    // =========================================================
    // VÉRIFIER ÉCOLE
    // =========================================================

    private void verifierEcole(Long ecoleId) {

        if (ecoleId == null) {
            throw new IllegalArgumentException(
                    "L'école est obligatoire"
            );
        }

        if (!ecoleRepository.existsById(ecoleId)) {
            throw new RuntimeException(
                    "École introuvable"
            );
        }
    }
}

