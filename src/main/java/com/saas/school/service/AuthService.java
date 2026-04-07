package com.saas.school.service;

import com.saas.school.dto.LoginRequest;
import com.saas.school.dto.RegisterRequest;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Role;
import com.saas.school.entity.Utilisateur;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EcoleRepository ecoleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(RegisterRequest request) {

        // 1. créer école
        Ecole ecole = new Ecole();
        ecole.setNom(request.getNomEcole());
        ecole.setAdresse(request.getAdresse());
        ecole.setVille(request.getVille());
        ecole.setPays(request.getPays());
        ecole.setCreatedAt(LocalDateTime.now());
        ecole.setActive(true);
        ecole.setTelephone(request.getTelephone());

        ecoleRepository.save(ecole);

        // 2. créer admin
        Utilisateur user = new Utilisateur();
        user.setEmail(request.getEmail()); // ou emailAdmin si tu as séparé
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ADMIN);
        user.setEcole(ecole);

        utilisateurRepository.save(user);
    }


            public Map<String, Object> login(LoginRequest request) {

            // 1. Vérifier email
            Utilisateur user = utilisateurRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email incorrect"));

            // 2. Vérifier mot de passe
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("Mot de passe incorrect");
            }

            // 3. Retourner user + école
                return Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "role", user.getRole(), // 🔥 AJOUT ICI
                        "ecole", Map.of(
                                "id", user.getEcole().getId(),
                                "nom", user.getEcole().getNom()
                        )
                );
        }
    }

