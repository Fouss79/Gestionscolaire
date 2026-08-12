package com.saas.school.service;

import com.saas.school.entity.Cycle;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Niveau;
import com.saas.school.repository.CycleRepository;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.NiveauRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NiveauService {

    private final NiveauRepository niveauRepository;
    private final EcoleRepository ecoleRepository;
    private final CycleRepository cycleRepository;

    public List<Niveau> getByEcole(Long ecoleId) {
        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));
        return niveauRepository.findByEcole(ecole);
    }

    public Niveau create(String nom, Long ecoleId, Long cycleId) {

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        Niveau niveau = new Niveau();
        niveau.setNom(nom);
        niveau.setEcole(ecole);

        if (cycleId != null) {
            Cycle cycle = cycleRepository.findById(cycleId)
                    .orElseThrow(() -> new RuntimeException("Cycle introuvable"));
            niveau.setCycle(cycle);
        }

        return niveauRepository.save(niveau);
    }

    public Niveau modifier(Long id, String nom, Long cycleId) {

        Niveau niveau = niveauRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Niveau introuvable"));

        niveau.setNom(nom);

        if (cycleId != null) {
            Cycle cycle = cycleRepository.findById(cycleId)
                    .orElseThrow(() -> new RuntimeException("Cycle introuvable"));
            niveau.setCycle(cycle);
        } else {
            niveau.setCycle(null);
        }

        return niveauRepository.save(niveau);
    }
    public Niveau modifier(Long id, String nom) {
        Niveau niveau = niveauRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Niveau introuvable"));
        niveau.setNom(nom);
        return niveauRepository.save(niveau);
    }
}