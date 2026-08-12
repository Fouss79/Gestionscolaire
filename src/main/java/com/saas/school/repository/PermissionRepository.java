package com.saas.school.repository;

import com.saas.school.entity.Permission;
import com.saas.school.entity.Presence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    boolean existsByCode(String code);

    List<Permission> findAllByCodeIn(List<String> codes);
}