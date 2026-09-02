package com.saas.school.repository;

import com.saas.school.entity.Depense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepenseRepository
        extends JpaRepository<Depense, Long> {

    List<Depense> findByEcole_IdOrderByDateDepenseDesc(Long ecoleId);
}