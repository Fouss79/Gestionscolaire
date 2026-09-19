package com.saas.school.repository;

import com.saas.school.entity.RepartitionEpreuve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepartitionEpreuveRepository
        extends JpaRepository<RepartitionEpreuve, Long> {

    List<RepartitionEpreuve> findByEpreuveId(Long epreuveId);

    List<RepartitionEpreuve> findBySalleId(Long salleId);

    boolean existsByEpreuveIdAndInscriptionId(
            Long epreuveId,
            Long inscriptionId
    );

    void deleteByEpreuveId(Long epreuveId);
}