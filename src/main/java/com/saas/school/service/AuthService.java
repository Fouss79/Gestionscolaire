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

import java.time.LocalDate;
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

        Ecole ecole = user.getEcole();

        // 🔥 3. Vérifier si école désactivée
        if (!ecole.isActive()) {
            throw new RuntimeException("École désactivée. Contactez le support.");
        }

        // 🔥 4. Vérifier expiration abonnement
        if (ecole.getDateFin() != null && ecole.getDateFin().isBefore(LocalDate.now())) {
            throw new RuntimeException("Abonnement expiré. Renouvelez votre abonnement.");
        }

        // 5. Retour
        return Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "ecole", Map.of(
                        "id", ecole.getId(),
                        "nom", ecole.getNom(),
                        "plan", ecole.getPlan(),
                        "dateFin", ecole.getDateFin()
                )
        );
    } }

