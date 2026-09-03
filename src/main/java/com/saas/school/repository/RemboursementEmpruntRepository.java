package com.saas.school.repository;

import com.saas.school.entity.RemboursementEmprunt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RemboursementEmpruntRepository
        extends JpaRepository<RemboursementEmprunt, Long> {

    List<RemboursementEmprunt>
    findByEmprunt_IdOrderByDateRemboursementDesc(Long empruntId);

    List<RemboursementEmprunt>
    findByEmprunt_Ecole_IdOrderByDateRemboursementDesc(Long ecoleId);
}