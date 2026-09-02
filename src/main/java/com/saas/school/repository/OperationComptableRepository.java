package com.saas.school.repository;

import com.saas.school.entity.OperationComptable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationComptableRepository
        extends JpaRepository<OperationComptable, Long> {

    boolean existsByPaiementScolarite_Id(Long paiementId);

    List<OperationComptable>
    findByEcole_IdOrderByDateOperationDesc(Long ecoleId);

    boolean existsByPaiementDepense_Id(Long id);
}

