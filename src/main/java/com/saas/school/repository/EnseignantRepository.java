package com.saas.school.repository;

import com.saas.school.entity.Enseignant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnseignantRepository extends JpaRepository<Enseignant, Long> {
    List<Enseignant> findByEcoleId(Long ecoleId);
    long countByEcoleId(Long ecoleId);
}