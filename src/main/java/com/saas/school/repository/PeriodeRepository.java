package com.saas.school.repository;

import com.saas.school.entity.Periode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PeriodeRepository extends JpaRepository<Periode, Long> {
    List<Periode> findByEcoleIdAndAnneeScolaireIdOrderByOrdreAsc(Long ecoleId, Long anneeScolaireId);
    @Query("""
    SELECT p FROM Periode p
    WHERE p.anneeScolaire.id = :anneeScolaireId
    AND :date BETWEEN p.dateDebut AND p.dateFin
    """)
    Optional<Periode> findByDate(@Param("anneeScolaireId") Long anneeScolaireId, @Param("date") LocalDate date);

}