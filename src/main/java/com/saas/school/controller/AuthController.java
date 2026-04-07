package com.saas.school.controller;

import com.saas.school.dto.LoginRequest;
import com.saas.school.dto.RegisterRequest;
import com.saas.school.entity.Utilisateur;
import com.saas.school.repository.UtilisateurRepository;
import com.saas.school.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UtilisateurRepository utilisateurRepository;

    // 🔥 REGISTER
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        authService.register(request);

        return ResponseEntity.ok(Map.of(
                "message", "Inscription réussie"
        ));
    }
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // 📌 GET ALL USERS
    @GetMapping("/users")
    public ResponseEntity<List<Utilisateur>> getAllUsers() {
        return ResponseEntity.ok(utilisateurRepository.findAll());
    }

    // 📌 GET USER BY ID
    @GetMapping("/users/{id}")
    public ResponseEntity<Utilisateur> getUserById(@PathVariable Long id) {
        return utilisateurRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}