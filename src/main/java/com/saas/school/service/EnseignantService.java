package com.saas.school.service;

import com.saas.school.entity.Enseignant;
import com.saas.school.repository.EnseignantRepository;
import com.saas.school.repository.EcoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnseignantService {

    private final EnseignantRepository enseignantRepository;
    private final EcoleRepository ecoleRepository;

    public Enseignant create(String nom, String prenom, String tel, Long ecoleId, String specialite) {
        Enseignant e = new Enseignant();
        e.setNom(nom);
        e.setPrenom(prenom);
        e.setTelephone(tel);
        e.setSpecialite(specialite);
        e.setEcole(ecoleRepository.findById(ecoleId).orElseThrow());

        return enseignantRepository.save(e);
    }

    public List<Enseignant> getByEcole(Long ecoleId) {
        return enseignantRepository.findByEcoleId(ecoleId);
    }
}