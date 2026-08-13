package com.saas.school.service;

import com.saas.school.entity.Utilisateur;
import com.saas.school.repository.UtilisateurRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!token.equals("null") && !token.isEmpty()) {
            try {
                String email = jwtService.extractEmail(token);
                List<String> permissions = jwtService.extractPermissions(token);

                Utilisateur user = utilisateurRepository.findByEmail(email).orElse(null);

                if (user != null) {
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getNom()));

                    if (permissions != null && !permissions.isEmpty()) {
                        permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
                    }

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                // Erreur de parsing/validation du JWT uniquement — pas les erreurs métier en aval
                System.out.println("❌ JWT ERROR: " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        // ✅ Un seul appel, en dehors de tout try-catch qui pourrait avaler des exceptions métier
        filterChain.doFilter(request, response);
    }
}