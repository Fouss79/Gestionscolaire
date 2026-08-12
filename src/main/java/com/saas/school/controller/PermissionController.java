package com.saas.school.controller;

import com.saas.school.entity.Permission;
import com.saas.school.entity.Role;
import com.saas.school.repository.PermissionRepository;
import com.saas.school.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @GetMapping
    public List<Permission> getAll() {
        return permissionRepository.findAll();
    }

    @PutMapping("/{id}/permissions")
    public Role updatePermissions(
            @PathVariable Long id,
            @RequestBody List<Long> permissionIds
    ) {

        Role role = roleRepository.findById(id)
                .orElseThrow();

        List<Permission> permissions =
                permissionRepository.findAllById(permissionIds);

        role.setPermissions(permissions);

        return roleRepository.save(role);
    }
}