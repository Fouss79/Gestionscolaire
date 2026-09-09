package com.saas.school.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import com.saas.school.entity.PaiementEnseignant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PaiementEnseignantRepository extends JpaRepository<PaiementEnseignant, Long> {

    List<PaiementEnseignant> findByAnneeScolaireId(Long anneeId);

    List<PaiementEnseignant> findByEnseignant_IdAndAnneeScolaireId(Long enseignantId, Long anneeId);

    boolean existsByEnseignant_IdAndPeriodeDebutAndPeriodeFin(
            Long enseignantId, LocalDate periodeDebut, LocalDate periodeFin);

    @Query("""
    SELECT COUNT(p) > 0
    FROM PaiementEnseignant p
    WHERE p.enseignant.id = :enseignantId
      AND p.anneeScolaireId = :anneeScolaireId
      AND p.salaireBase > 0
      AND p.periodeDebut <= :fin
      AND p.periodeFin >= :debut
""")
    boolean existsChevauchementSalaireFixe(
            @Param("enseignantId") Long enseignantId,
            @Param("anneeScolaireId") Long anneeScolaireId,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin
    );

}