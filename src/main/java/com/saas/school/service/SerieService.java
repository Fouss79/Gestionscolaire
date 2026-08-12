package com.saas.school.service;

import com.saas.school.entity.Ecole;
import com.saas.school.entity.Niveau;
import com.saas.school.entity.Serie;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.NiveauRepository;
import com.saas.school.repository.SerieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SerieService {

    private final SerieRepository serieRepository;
    private final EcoleRepository ecoleRepository;

    // 🔥 Créer un niveau pour une école
    public Serie creerSerie(String nom, Long ecoleId) {

        // 1. Vérifier si l'école existe
        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        // 2. Vérifier si le niveau existe déjà pour cette école
        if (serieRepository.existsByNomAndEcoleId(nom, ecoleId)) {
            throw new RuntimeException("Cette serie existe déjà pour cette école");
        }

        // 3. Créer niveau
        Serie serie = new Serie();
        serie.setNom(nom);
        serie.setEcole(ecole);

        return serieRepository.save(serie);
    }

    // 📥 Récupérer tous les niveaux d’une école
    public List<Serie> getSeriesByEcole(Long ecoleId) {
        return serieRepository.findByEcoleId(ecoleId);
    }

    // 📥 Récupérer un niveau par ID
    public Serie getById(Long id) {
        return serieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serie introuvable"));
    }

    // ❌ Supprimer un niveau
    public void delete(Long id) {
        serieRepository.deleteById(id);
    }
    public Serie modifier(Long id, String nom) {
        Serie serie = serieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Série introuvable"));
        serie.setNom(nom);
        return serieRepository.save(serie);
    }
}
