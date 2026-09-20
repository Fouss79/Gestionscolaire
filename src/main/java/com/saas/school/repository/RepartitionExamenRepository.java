package com.saas.school.repository;

import com.saas.school.entity.RepartitionExamen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepartitionExamenRepository
        extends JpaRepository<RepartitionExamen, Long> {

    /**
     * Récupère toute la répartition d'un examen.
     */
    List<RepartitionExamen> findByExamenId(Long examenId);

    /**
     * Récupère les élèves affectés à une salle.
     */
    List<RepartitionExamen> findBySalleId(Long salleId);

    /**
     * Vérifie si un élève possède déjà une salle
     * pour cet examen.
     */
    boolean existsByExamenIdAndInscriptionId(
            Long examenId,
            Long inscriptionId
    );

    /**
     * Supprime toute la répartition d'un examen.
     *
     * Utilisé avant de recalculer automatiquement
     * la répartition.
     */
    void deleteByExamenId(Long examenId);
}