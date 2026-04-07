package com.saas.school.repository;

import com.saas.school.entity.BesoinHeure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BesoinHeureRepository extends JpaRepository<BesoinHeure, Long> {

    List<BesoinHeure> findByClasseIdAndAnneeScolaireId(Long classeId, Long anneeId);
}
