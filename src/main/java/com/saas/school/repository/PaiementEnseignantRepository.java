package com.saas.school.repository;

import com.saas.school.entity.PaiementEnseignant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PaiementEnseignantRepository extends JpaRepository<PaiementEnseignant, Long> {

    List<PaiementEnseignant> findByAnneeScolaireId(Long anneeId);

    List<PaiementEnseignant> findByEnseignant_IdAndAnneeScolaireId(Long enseignantId, Long anneeId);

    boolean existsByEnseignant_IdAndPeriodeDebutAndPeriodeFin(
            Long enseignantId, LocalDate periodeDebut, LocalDate periodeFin);
}