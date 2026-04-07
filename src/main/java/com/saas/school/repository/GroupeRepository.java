
package com.saas.school.repository;

import com.saas.school.entity.Groupe;
import com.saas.school.entity.Niveau;
import com.saas.school.entity.Serie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupeRepository extends JpaRepository<Groupe, Long> {

    Optional<Groupe> findByNomAndEcoleId(String nom, Long ecoleId);

    List<Groupe> findByEcoleId(Long ecoleId);
    boolean existsByNomAndEcoleId(String nom, Long ecoleId);
}