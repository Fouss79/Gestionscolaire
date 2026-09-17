package com.saas.school.repository;

import com.saas.school.entity.RepartitionExamen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepartitionExamenRepository
        extends JpaRepository<RepartitionExamen, Long> {

    List<RepartitionExamen> findByExamenIdOrderBySalleIdAscNumeroPlaceAsc(
            Long examenId
    );

    List<RepartitionExamen> findByExamenIdAndSalleIdOrderByNumeroPlaceAsc(
            Long examenId,
            Long salleId
    );

    Optional<RepartitionExamen> findByExamenIdAndInscriptionId(
            Long examenId,
            Long inscriptionId
    );

    boolean existsByExamenIdAndInscriptionId(
            Long examenId,
            Long inscriptionId
    );

    boolean existsByExamenIdAndSalleIdAndNumeroPlace(
            Long examenId,
            Long salleId,
            Integer numeroPlace
    );

    long countByExamenIdAndSalleId(
            Long examenId,
            Long salleId
    );

    void deleteByExamenId(Long examenId);
}