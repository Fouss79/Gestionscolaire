package com.saas.school.repository;

import com.saas.school.entity.Habilitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface HabilitationRepository extends JpaRepository<Habilitation, Long> {

    boolean existsByEnseignantIdAndMatiereId(
            Long enseignantId,
            Long matiereId
    );


    List<Habilitation> findByMatiereId(Long matiereId);


    List<Habilitation> findByEcoleId(Long ecoleId);
}