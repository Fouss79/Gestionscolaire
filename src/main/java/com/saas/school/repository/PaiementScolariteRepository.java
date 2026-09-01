
package com.saas.school.repository;

import com.saas.school.entity.PaiementScolarite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaiementScolariteRepository extends JpaRepository<PaiementScolarite, Long> {

    List<PaiementScolarite> findByLigneFrais_Inscription_IdOrderByDatePaiementDesc(Long inscriptionId);

    List<PaiementScolarite> findByLigneFraisIdOrderByDatePaiementDesc(Long ligneFraisId);

    List<PaiementScolarite> findByLigneFrais_Inscription_Ecole_IdOrderByDatePaiementDesc(Long ecoleId);

    List<PaiementScolarite> findByLigneFraisId(Long ligneFraisId);
    List<PaiementScolarite> findByLigneFrais_Inscription_IdOrderByDatePaiementAsc(
            Long inscriptionId);


}