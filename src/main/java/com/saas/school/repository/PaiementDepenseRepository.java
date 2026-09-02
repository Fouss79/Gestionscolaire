package com.saas.school.repository;

import com.saas.school.entity.PaiementDepense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaiementDepenseRepository extends JpaRepository<PaiementDepense, Long> {

    List<PaiementDepense> findByDepense_IdOrderByDatePaiementDesc(Long depenseId);

    List<PaiementDepense> findByDepense_Ecole_IdOrderByDatePaiementDesc(Long ecoleId);
}