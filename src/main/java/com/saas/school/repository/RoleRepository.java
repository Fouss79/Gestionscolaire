package com.saas.school.repository;

import com.saas.school.entity.Ecole;
import com.saas.school.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Role findByNom(String nom);
    Optional<Role> findByNomAndEcoleId(String nom, Long ecoleId);

    List<Role> findByEcoleId(Long ecoleId);

    Optional<Role> findByNomAndEcole(String nomRole, Ecole ecole);
    @Query("SELECT r FROM Role r JOIN FETCH r.permissions WHERE r.id = :id")
    Role findByIdWithPermissions(Long id);
}