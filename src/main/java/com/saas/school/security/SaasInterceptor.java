package com.saas.school.security;

import com.saas.school.entity.Ecole;
import com.saas.school.entity.Utilisateur;
import com.saas.school.repository.UtilisateurRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SaasInterceptor implements HandlerInterceptor {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String email = request.getHeader("X-USER-EMAIL"); // 🔥 temporaire sans JWT

        if (email == null) {
            return true; // laisser passer si pas connecté (ex: login)
        }

        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Ecole ecole = user.getEcole();

        // 🔴 école désactivée
        if (!ecole.isActive()) {
            response.sendError(403, "École désactivée");
            return false;
        }

        // 🔴 abonnement expiré
        if (ecole.getDateFin() != null &&
                ecole.getDateFin().isBefore(LocalDate.now())) {

            response.sendError(403, "Abonnement expiré");
            return false;
        }
        if (request.getRequestURI().contains("/api/paiements/callback")) {
            return true;
        }

        return true;
    }
}