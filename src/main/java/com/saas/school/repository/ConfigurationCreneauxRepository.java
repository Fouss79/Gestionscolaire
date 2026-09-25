package com.saas.school.repository;

import com.saas.school.entity.ConfigurationCreneaux;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConfigurationCreneauxRepository
        extends JpaRepository<ConfigurationCreneaux, Long> {

    Optional<ConfigurationCreneaux>
    findByEcoleIdAndCycleId(Long ecoleId, Long cycleId);
}