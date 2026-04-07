package com.saas.school.service;

import com.saas.school.entity.Ecole;
import com.saas.school.entity.Niveau;
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

    public List<Niveau> getByEcole(Long ecoleId) {
        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));
        return niveauRepository.findByEcole(ecole);
    }

    public Niveau create(String nom, Long ecoleId) {
        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));
        Niveau niveau = new Niveau();
        niveau.setNom(nom);
        niveau.setEcole(ecole);
        return niveauRepository.save(niveau);
    }
}