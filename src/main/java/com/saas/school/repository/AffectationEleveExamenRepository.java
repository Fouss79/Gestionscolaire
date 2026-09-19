package com.saas.school.repository;

import com.saas.school.entity.AffectationEleveExamen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AffectationEleveExamenRepository extends JpaRepository<AffectationEleveExamen, Long> {

    List<AffectationEleveExamen> findByExamenId(Long examenId);

    List<AffectationEleveExamen> findByExamenIdAndSalleId(Long examenId, Long salleId);

    Optional<AffectationEleveExamen> findByExamenIdAndInscriptionId(Long examenId, Long inscriptionId);

    boolean existsByExamenIdAndInscriptionId(Long examenId, Long inscriptionId);

    long countByExamenIdAndSalleId(Long examenId, Long salleId);

    // Nécessaire pour régénérer une répartition proprement (tout supprimer avant de réinsérer).
    void deleteByExamenId(Long examenId);
}