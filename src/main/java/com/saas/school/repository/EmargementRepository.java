package com.saas.school.repository;

import com.saas.school.entity.Emargement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmargementRepository extends JpaRepository<Emargement, Long> {

    boolean existsByEmploiDuTemps_IdAndDateHeure(Long emploiId, LocalDate date);

    List<Emargement> findByDateHeure(LocalDate date);

    boolean existsByEmploiDuTempsId(Long emploiDuTempsId);

    long countByEmploiDuTempsId(Long emploiDuTempsId);

    void deleteByEmploiDuTempsId(Long emploiDuTempsId);
}