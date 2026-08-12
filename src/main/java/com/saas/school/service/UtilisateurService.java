package com.saas.school.service;

import com.saas.school.entity.Role;
import com.saas.school.entity.Utilisateur;
import com.saas.school.repository.RoleRepository;
import com.saas.school.repository.UtilisateurRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;


    public Utilisateur save(Utilisateur user) {
        return utilisateurRepository.save(user);
    }

    @Transactional
    public Utilisateur changerRole(Long utilisateurId, Long roleId) {

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role introuvable"));

        utilisateur.setRole(role);

        return utilisateurRepository.save(utilisateur);
    }

    public Utilisateur findByEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    public List<Utilisateur> getAll() {
        return utilisateurRepository.findAll();
    }
}
