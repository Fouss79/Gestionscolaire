package com.saas.school.repository;

import com.saas.school.entity.Cycle;
import com.saas.school.entity.Ecole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CycleRepository extends JpaRepository<Cycle, Long> {
    List<Cycle> findByEcoleId(Long ecoleId);

    Optional<Cycle> findByNomAndEcole(String nomCycle, Ecole ecole);
}