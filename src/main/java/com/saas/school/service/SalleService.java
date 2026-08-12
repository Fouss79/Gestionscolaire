package com.saas.school.service;

import com.saas.school.dto.Request;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Salle;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.SalleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SalleService {

    private final SalleRepository salleRepository;
    private final EcoleRepository ecoleRepository;

    public Salle creer(String nom, Integer capacite, Long ecoleId) {

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        Salle salle = new Salle();
        salle.setNom(nom);
        salle.setCapacite(capacite);
        salle.setEcole(ecole);

        return salleRepository.save(salle);
    }

    public List<Salle> getByEcole(Long ecoleId) {
        return salleRepository.findByEcoleId(ecoleId);
    }

    public Salle modifier(Long id, String nom, Integer capacite) {
        Salle salle = salleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Salle introuvable"));
        salle.setNom(nom);
        salle.setCapacite(capacite);
        return salleRepository.save(salle);
    }

    public void supprimer(Long id) {
        if (!salleRepository.existsById(id)) {
            throw new RuntimeException("Salle introuvable");
        }
        salleRepository.deleteById(id);
    }
}