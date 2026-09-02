package com.saas.school.repository;

import com.saas.school.entity.CategorieDepense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategorieDepenseRepository
        extends JpaRepository<CategorieDepense, Long> {

    List<CategorieDepense> findByEcole_IdOrderByNomAsc(Long ecoleId);

    boolean existsByNomIgnoreCaseAndEcole_Id(
            String nom,
            Long ecoleId
    );

    boolean existsByEcole_IdAndNomIgnoreCase(Long ecoleId, String nom);

    List<CategorieDepense> findByEcole_Id(Long ecoleId);
}