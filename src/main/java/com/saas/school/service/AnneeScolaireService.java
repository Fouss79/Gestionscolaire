package com.saas.school.service;

import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Ecole;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.EcoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnneeScolaireService {

    private final AnneeScolaireRepository anneeRepository;
    private final EcoleRepository ecoleRepository;

    // 🔥 créer année scolaire
    public AnneeScolaire creer(String nom, Long ecoleId) {

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        // vérifier doublon
        if (anneeRepository.existsByNomAndEcoleId(nom, ecoleId)) {
            throw new RuntimeException("Cette année existe déjà pour cette école");
        }

        AnneeScolaire annee = new AnneeScolaire();
        annee.setNom(nom);
        annee.setEcole(ecole);
        annee.setActive(false);
        annee.setCreatedAt(LocalDateTime.now());

        return anneeRepository.save(annee);
    }

    // 📥 toutes les années d'une école
    public List<AnneeScolaire> getByEcole(Long ecoleId) {
        return anneeRepository.findByEcoleId(ecoleId);
    }

    // 🔥 activer une année (une seule active)
    public AnneeScolaire activer(Long id) {

        AnneeScolaire annee = anneeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Année introuvable"));

        // désactiver les autres
        List<AnneeScolaire> annees = anneeRepository.findByEcoleId(annee.getEcole().getId());
        for (AnneeScolaire a : annees) {
            a.setActive(false);
        }
        anneeRepository.saveAll(annees);

        // activer celle-ci
        annee.setActive(true);
        return anneeRepository.save(annee);
    }

    // 📥 année active
    public AnneeScolaire getActive(Long ecoleId) {
        return anneeRepository.findByEcoleIdAndActiveTrue(ecoleId)
                .orElseThrow(() -> new RuntimeException("Aucune année active"));
    }
}