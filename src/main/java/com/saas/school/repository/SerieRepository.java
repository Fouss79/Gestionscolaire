
package com.saas.school.repository;

import com.saas.school.entity.Niveau;
import com.saas.school.entity.Serie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SerieRepository extends JpaRepository<Serie, Long> {

    Optional<Serie> findByNomAndEcoleId(String nom, Long ecoleId);

    List<Serie> findByEcoleId(Long ecoleId);
    boolean existsByNomAndEcoleId(String nom, Long ecoleId);
}