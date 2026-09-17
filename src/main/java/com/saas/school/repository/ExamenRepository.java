package com.saas.school.repository;

import com.saas.school.entity.Examen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamenRepository
        extends JpaRepository<Examen, Long> {

    List<Examen> findByEcoleIdOrderByDateExamenDesc(
            Long ecoleId
    );

    List<Examen> findByAnneeScolaireIdOrderByDateExamenAsc(
            Long anneeId
    );

    List<Examen> findByEcoleIdAndAnneeScolaireIdOrderByDateExamenAsc(
            Long ecoleId,
            Long anneeId
    );
}