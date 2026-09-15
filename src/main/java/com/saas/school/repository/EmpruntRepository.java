package com.saas.school.repository;

import com.saas.school.entity.Emprunt;
import com.saas.school.service.StatutPaiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmpruntRepository extends JpaRepository<Emprunt, Long> {

    // Tous les emprunts d'une école
    List<Emprunt> findByEcole_IdOrderByDateEmpruntDesc(
            Long ecoleId
    );

    // Emprunts d'une école selon le statut
    List<Emprunt> findByEcole_IdAndStatutPaiementOrderByDateEmpruntDesc(
            Long ecoleId,
            StatutPaiement statutPaiement
    );

    // Ancienne recherche par période
    // Peut être conservée si elle est encore utilisée ailleurs
    List<Emprunt> findByEcole_IdAndDateEmpruntBetweenOrderByDateEmpruntDesc(
            Long ecoleId,
            LocalDateTime debut,
            LocalDateTime fin
    );

    // Emprunts d'une école pour une année scolaire
    List<Emprunt> findByEcole_IdAndAnneeScolaire_IdOrderByDateEmpruntDesc(
            Long ecoleId,
            Long anneeId
    );
}