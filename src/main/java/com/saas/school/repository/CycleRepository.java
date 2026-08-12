package com.saas.school.repository;

import com.saas.school.entity.Cycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CycleRepository extends JpaRepository<Cycle, Long> {
    List<Cycle> findByEcoleId(Long ecoleId);
}