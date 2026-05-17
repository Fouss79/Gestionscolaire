package com.saas.school.repository;

import com.saas.school.entity.Abonnement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {

    // 🔥 récupérer le dernier abonnement (le plus récent)
    Optional<Abonnement> findTopByEcoleIdOrderByDateFinDesc(Long ecoleId);

}

