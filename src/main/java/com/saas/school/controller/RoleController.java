package com.saas.school.controller;

import com.saas.school.entity.Permission;
import com.saas.school.entity.Role;
import com.saas.school.repository.RoleRepository;
import com.saas.school.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    // 🔥 1. Liste des rôles (IMPORTANT pour ton frontend)
    @GetMapping
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }
    @GetMapping("/ecole/{ecoleId}")
    public List<Role> getRolesParEcole(@PathVariable Long ecoleId) {
        return roleRepository.findByEcoleId(ecoleId);
    }

    // 🔥 2. Détail d’un rôle
    @GetMapping("/{id}")
    public Role getRole(@PathVariable Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role introuvable"));
    }

    // 🔥 3. UPDATE permissions d’un rôle
    @PutMapping("/{id}/permissions")
    public Role updatePermissions(
            @PathVariable Long id,
            @RequestBody List<Long> permissionIds
    ) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role introuvable"));

        role.setPermissions(
                permissionRepository.findAllById(permissionIds)
        );

        return roleRepository.save(role);
    }
    @PostMapping
    public Role creerRole(@RequestBody Role role) {

        if (roleRepository.findByNomAndEcole(role.getNom(), role.getEcole()).isPresent()) {
            throw new RuntimeException("Ce rôle existe déjà");
        }

        return roleRepository.save(role);
    }

    @PutMapping("/{id}")
    public Role modifierRole(
            @PathVariable Long id,
            @RequestBody Role request
    ) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rôle introuvable"));

        role.setNom(request.getNom());

        return roleRepository.save(role);
    }
    @DeleteMapping("/{id}")
    public void supprimerRole(@PathVariable Long id) {

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rôle introuvable"));

        roleRepository.delete(role);
    }
    @GetMapping("/{id}/permissions")
    public List<Permission> getPermissions(@PathVariable Long id) {

        Role role = roleRepository.findByIdWithPermissions(id);

        return role.getPermissions();
    }
}