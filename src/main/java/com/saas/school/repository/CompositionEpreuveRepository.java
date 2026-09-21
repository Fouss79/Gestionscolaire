package com.saas.school.repository;

import com.saas.school.entity.CompositionEpreuve;
import com.saas.school.entity.StatutComposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompositionEpreuveRepository
        extends JpaRepository<CompositionEpreuve, Long> {

    Optional<CompositionEpreuve> findByEpreuveIdAndInscriptionId(
            Long epreuveId,
            Long inscriptionId
    );

    List<CompositionEpreuve> findByEpreuveId(Long epreuveId);

    List<CompositionEpreuve> findByEpreuveIdAndStatut(
            Long epreuveId,
            StatutComposition statut
    );

    boolean existsByEpreuveIdAndInscriptionId(
            Long epreuveId,
            Long inscriptionId
    );


}