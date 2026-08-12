package com.saas.school.service;

import com.saas.school.entity.Utilisateur;
import com.saas.school.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final UtilisateurRepository utilisateurRepository;

    public boolean hasPermission(Long userId, String permissionCode) {

        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User introuvable"));

        return user.getRole()
                .getPermissions()
                .stream()
                .anyMatch(p -> p.getCode().equals(permissionCode));
    }
}