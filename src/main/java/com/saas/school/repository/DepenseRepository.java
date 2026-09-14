package com.saas.school.repository;

import com.saas.school.entity.Depense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DepenseRepository
        extends JpaRepository<Depense, Long> {

    List<Depense> findByEcole_IdOrderByDateDepenseDesc(Long ecoleId);

    List<Depense> findByEcole_IdAndDateDepenseBetweenOrderByDateDepenseDesc(
            Long ecoleId,
            LocalDate debut,
            LocalDate fin
    );

    List<Depense> findByEcole_IdAndAnneeScolaire_IdOrderByDateDepenseDesc(
            Long ecoleId,
            Long anneeId
    );
}