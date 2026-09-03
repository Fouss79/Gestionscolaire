package com.saas.school.repository;

import com.saas.school.entity.Emprunt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpruntRepository extends JpaRepository<Emprunt, Long> {

    List<Emprunt> findByEcole_IdOrderByDateEmpruntDesc(Long ecoleId);

    List<Emprunt> findByEcole_IdAndStatutPaiementOrderByDateEmpruntDesc(
            Long ecoleId,
            com.saas.school.service.StatutPaiement statutPaiement
    );
}