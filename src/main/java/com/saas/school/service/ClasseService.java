package com.saas.school.service;

import com.saas.school.dto.ClasseRequest;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClasseService {

    private final ClasseRepository classeRepository;
    private final NiveauRepository niveauRepository;
    private final SerieRepository serieRepository;
    private final GroupeRepository groupeRepository;
    private final EcoleRepository ecoleRepository;
    private final AbonnementService abonnementService;

    public Classe creerClasse(ClasseRequest request) {

        // 🔍 récupérer les entités
        Niveau niveau = niveauRepository.findById(request.getNiveauId())
                .orElseThrow(() -> new RuntimeException("Niveau introuvable"));

        Serie serie = serieRepository.findById(request.getSerieId())
                .orElseThrow(() -> new RuntimeException("Série introuvable"));

        Groupe groupe = groupeRepository.findById(request.getGroupeId())
                .orElseThrow(() -> new RuntimeException("Groupe introuvable"));

        Ecole ecole = ecoleRepository.findById(request.getEcoleId())
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        // 🔥 créer classe
        Classe classe = new Classe();
        classe.setNiveau(niveau);
        classe.setSerie(serie);
        classe.setGroupe(groupe);
        classe.setEcole(ecole);
        if (!abonnementService.isActif(ecole)) {
            throw new RuntimeException("🚫 Abonnement expiré");
        }

        return classeRepository.save(classe);
    }

    // 📥 Récupérer toutes les classes d’une école
    public List<Classe> getClasseByEcole(Long ecoleId) {
        return classeRepository.findByEcoleId(ecoleId);
    }



}