package com.saas.school.controller;

import com.saas.school.dto.ChangerRoleRequest;
import com.saas.school.entity.Role;
import com.saas.school.entity.Utilisateur;
import com.saas.school.repository.RoleRepository;
import com.saas.school.repository.UtilisateurRepository;
import com.saas.school.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurService utilisateurService;
    private final RoleRepository roleRepository;


    @GetMapping
    public List<Utilisateur> getAll() {
        return utilisateurRepository.findAll();
    }
    @PutMapping("/changer-role")
    public Utilisateur changerRole(@RequestBody ChangerRoleRequest request) {

        return utilisateurService.changerRole(
                request.getUtilisateurId(),
                request.getRoleId()
        );
    }
    @GetMapping("/ecole/{ecoleId}/utilisateurs")
    public List<Utilisateur> getUtilisateurs(@PathVariable Long ecoleId) {

        return utilisateurRepository.findByEcoleId(ecoleId);
    }
    @GetMapping("/roles")
    public List<Role> getRoles() {
        return roleRepository.findAll();
    }

    @GetMapping("/{id}")
    public Utilisateur getById(@PathVariable Long id) {
        return utilisateurRepository.findById(id).orElse(null);
    }
}
