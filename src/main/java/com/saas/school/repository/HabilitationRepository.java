package com.saas.school.repository;

import com.saas.school.entity.Habilitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface HabilitationRepository extends JpaRepository<Habilitation, Long> {
    List<Habilitation> findByEcoleIdAndAnneeScolaireId(Long ecoleId, Long anneeId);
    boolean existsByEnseignantIdAndMatiereIdAndAnneeScolaireId(
            Long enseignantId,
            Long matiereId,
            Long anneeScolaireId
    );

    Optional<Habilitation> findFirstByMatiereIdAndAnneeScolaireId(
            Long matiereId,
            Long anneeId
    );
    List<Habilitation> findByMatiereIdAndAnneeScolaireId(Long matiereId, Long anneeId);


    List<Habilitation> findByEcoleId(Long ecoleId);
}