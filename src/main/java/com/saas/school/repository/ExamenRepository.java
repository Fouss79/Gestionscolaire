package com.saas.school.repository;

import com.saas.school.entity.Examen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ExamenRepository extends JpaRepository<Examen, Long> {

    List<Examen> findByEcoleId(Long ecoleId);

    List<Examen> findByEcoleIdAndAnneeScolaireId(Long ecoleId, Long anneeScolaireId);
}