package com.saas.school.repository;

import com.saas.school.entity.Matiere;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatiereRepository extends JpaRepository<Matiere, Long> {
    List<Matiere> findByEcoleId(Long ecoleId);
    long countByEcoleId(Long ecoleId);
}