package com.saas.school.repository;

import com.saas.school.entity.Affectation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AffectationRepository extends JpaRepository<Affectation, Long> {

    boolean existsByClasseIdAndMatiereIdAndAnneeScolaireId(
            Long classeId,
            Long matiereId,
            Long anneeId
    );

    List<Affectation> findByClasseIdAndAnneeScolaireId(Long classeId, Long anneeId);

    List<Affectation> findByEnseignantId(Long enseignantId);

    Optional<Affectation> findByClasseIdAndMatiereIdAndAnneeScolaireId(
            Long classeId,
            Long matiereId,
            Long anneeId
    );
    List <Affectation> findByAnneeScolaireId(Long anneeId);
}
