package com.saas.school.repository;

import com.saas.school.entity.CreneauExamen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CreneauExamenRepository extends JpaRepository<CreneauExamen, Long> {

    List<CreneauExamen> findByExamenIdOrderByDateAscHeureDebutAsc(Long examenId);
}