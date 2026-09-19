package com.saas.school.repository;

import com.saas.school.entity.EpreuveSalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EpreuveSalleRepository
        extends JpaRepository<EpreuveSalle, Long> {

    List<EpreuveSalle> findByEpreuveId(Long epreuveId);

    Optional<EpreuveSalle> findByEpreuveIdAndSalleId(
            Long epreuveId,
            Long salleId
    );

    boolean existsByEpreuveIdAndSalleId(
            Long epreuveId,
            Long salleId
    );

    void deleteByEpreuveIdAndSalleId(
            Long epreuveId,
            Long salleId
    );
}