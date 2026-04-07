package com.saas.school.service;

import com.saas.school.entity.Utilisateur;
import com.saas.school.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;

    public Utilisateur save(Utilisateur user) {
        return utilisateurRepository.save(user);
    }

    public Utilisateur findByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    public List<Utilisateur> getAll() {
        return utilisateurRepository.findAll();
    }
}
