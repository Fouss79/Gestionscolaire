package com.saas.school.service;

import com.saas.school.entity.Matiere;
import com.saas.school.repository.MatiereRepository;
import com.saas.school.repository.EcoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatiereService {

    private final MatiereRepository matiereRepository;
    private final EcoleRepository ecoleRepository;

    public Matiere create(String nom, Long ecoleId) {
        Matiere m = new Matiere();
        m.setNom(nom);
        m.setEcole(ecoleRepository.findById(ecoleId).orElseThrow());

        return matiereRepository.save(m);
    }

    public List<Matiere> getByEcole(Long ecoleId) {
        return matiereRepository.findByEcoleId(ecoleId);
    }
}