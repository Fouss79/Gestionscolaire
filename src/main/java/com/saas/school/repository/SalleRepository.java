package com.saas.school.repository;

import com.saas.school.entity.Salle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalleRepository extends JpaRepository<Salle, Long> {
    List<Salle> findByEcoleId(Long ecoleId);
}