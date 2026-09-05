package com.saas.school.repository;

import com.saas.school.entity.OperationComptable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OperationComptableRepository
        extends JpaRepository<OperationComptable, Long> {

    boolean existsByPaiementScolarite_Id(Long paiementId);

    List<OperationComptable>
    findByEcole_IdOrderByDateOperationDesc(Long ecoleId);

    List<OperationComptable>
    findByEcole_IdAndDateOperationBetweenOrderByDateOperationDesc(
            Long ecoleId, LocalDateTime debut, LocalDateTime fin
    );

    boolean existsByPaiementDepense_Id(Long id);

    boolean existsByRemboursementEmprunt_Id(Long remboursementId);

    boolean existsByEmprunt_Id(Long id);
}