package com.saas.school.repository;

import com.saas.school.entity.ExamenSalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamenSalleRepository extends JpaRepository<ExamenSalle, Long> {

    /**
     * Toutes les salles affectées à un examen.
     */
    List<ExamenSalle> findByExamenId(Long examenId);

    /**
     * Tous les examens utilisant une salle.
     */
    List<ExamenSalle> findBySalleId(Long salleId);

    /**
     * Vérifie si une salle est déjà affectée à un examen.
     */
    boolean existsByExamenIdAndSalleId(
            Long examenId,
            Long salleId
    );

    /**
     * Retire une salle d'un examen.
     */
    void deleteByExamenIdAndSalleId(
            Long examenId,
            Long salleId
    );

    /**
     * Retire toutes les salles d'un examen.
     */
    void deleteByExamenId(Long examenId);
}