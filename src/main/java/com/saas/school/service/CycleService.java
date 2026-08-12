package com.saas.school.service;

import com.saas.school.entity.Cycle;
import com.saas.school.entity.Ecole;
import com.saas.school.repository.CycleRepository;
import com.saas.school.repository.EcoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CycleService {

    private final CycleRepository cycleRepository;
    private final EcoleRepository ecoleRepository;

    public Cycle creer(String nom, Long ecoleId) {
        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        Cycle cycle = new Cycle();
        cycle.setNom(nom);
        cycle.setEcole(ecole);

        return cycleRepository.save(cycle);
    }

    public List<Cycle> getByEcole(Long ecoleId) {
        return cycleRepository.findByEcoleId(ecoleId);
    }

    public Cycle modifier(Long id, String nom) {
        Cycle cycle = cycleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cycle introuvable"));
        cycle.setNom(nom);
        return cycleRepository.save(cycle);
    }

    public void supprimer(Long id) {
        if (!cycleRepository.existsById(id)) {
            throw new RuntimeException("Cycle introuvable");
        }
        cycleRepository.deleteById(id);
    }
}