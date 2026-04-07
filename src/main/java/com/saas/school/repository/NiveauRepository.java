package com.saas.school.repository;

import com.saas.school.entity.Ecole;
import com.saas.school.entity.Niveau;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NiveauRepository extends JpaRepository<Niveau, Long> {
        List<Niveau> findByEcole(Ecole ecole);

    Optional<Niveau> findByNomAndEcoleId(String nom, Long ecoleId);

    List<Niveau> findByEcoleId(Long ecoleId);
    boolean existsByNomAndEcoleId(String nom, Long ecoleId);
}