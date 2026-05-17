package com.saas.school.service;

import com.saas.school.dto.EleveRequest;
import com.saas.school.entity.Classe;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Eleve;
import com.saas.school.entity.Inscription;
import com.saas.school.repository.ClasseRepository;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.EleveRepository;
import com.saas.school.repository.InscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class EleveService {

    private final EleveRepository eleveRepository;
    private final ClasseRepository classeRepository;
    private final EcoleRepository ecoleRepository;
    private final InscriptionRepository inscriptionRepository;

    // 🔥 Créer élève
    public Eleve creerEleve(EleveRequest request) {

        // 1. vérifier école
        Ecole ecole = ecoleRepository.findById(request.getEcoleId())
                .orElseThrow(() -> new RuntimeException("École introuvable"));
        Classe classe = classeRepository.findById(request.getClasseId()).orElseThrow(() -> new RuntimeException("École introuvable"));

        // 3. générer matricule
        long count = eleveRepository.count() + 1;
        String matricule = "ELV" + String.format("%04d", count);


        // 3. créer élève
        Eleve eleve = new Eleve();
        eleve.setNom(request.getNom());
        eleve.setPrenom(request.getPrenom());
        eleve.setDateNaissance(request.getDateNaissance());
        eleve.setSexe(request.getSexe());
        eleve.setMatricule(matricule);
        eleve.setCreatedAt(LocalDateTime.now());
        eleve.setActive(true);
        eleve.setEcole(ecole);
        eleve.setClasse(classe);


        return eleveRepository.save(eleve);
    }

    // 📥 élèves par classe
    public List<Eleve> getByClasse(Long classeId) {
        return eleveRepository.findByClasseId(classeId);
    }

    // 📥 élèves par école
    public List<Eleve> getByEcole(Long ecoleId) {
        return eleveRepository.findByEcoleId(ecoleId);
    }

    // 📥 par id
    public Eleve getById(Long id) {
        return eleveRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Élève introuvable"));
    }
}