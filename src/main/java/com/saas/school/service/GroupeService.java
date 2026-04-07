package com.saas.school.service;

import com.saas.school.entity.Ecole;
import com.saas.school.entity.Groupe;

import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.GroupeRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupeService {

    private final GroupeRepository groupeRepository;
    private final EcoleRepository ecoleRepository;

    // 🔥 Créer un groupe pour une école
    public Groupe creerGroupe(String nom, Long ecoleId) {

        // 1. Vérifier si l'école existe
        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        // 2. Vérifier si le niveau existe déjà pour cette école
        if (groupeRepository.existsByNomAndEcoleId(nom, ecoleId)) {
            throw new RuntimeException("Ce groupe existe déjà pour cette école");
        }

        // 3. Créer groupe
        Groupe groupe = new Groupe();
        groupe.setNom(nom);
        groupe.setEcole(ecole);

        return groupeRepository.save(groupe);
    }

    // 📥 Récupérer tous les niveaux d’une école
    public List<Groupe> getGroupesByEcole(Long ecoleId) {
        return groupeRepository.findByEcoleId(ecoleId);
    }

    // 📥 Récupérer un niveau par ID
    public Groupe getById(Long id) {
        return groupeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Niveau introuvable"));
    }

    // ❌ Supprimer un niveau
    public void delete(Long id) {
        groupeRepository.deleteById(id);
    }
}
