package com.saas.school.repository;

import com.saas.school.entity.AffectationExamen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AffectationExamenRepository
        extends JpaRepository<AffectationExamen, Long> {

    List<AffectationExamen> findByExamenIdOrderBySalleIdAscNumeroPlaceAsc(
            Long examenId
    );

    List<AffectationExamen> findByExamenIdAndSalleIdOrderByNumeroPlaceAsc(
            Long examenId,
            Long salleId
    );

    Optional<AffectationExamen>
    findByExamenIdAndInscriptionId(
            Long examenId,
            Long inscriptionId
    );

    long countByExamenIdAndSalleId(
            Long examenId,
            Long salleId
    );

    void deleteByExamenId(Long examenId);
}