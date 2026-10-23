
package com.saas.school.repository;

import com.saas.school.entity.EvaluationConduite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationConduiteRepository
        extends JpaRepository<EvaluationConduite, Long> {

    Optional<EvaluationConduite> findByInscriptionIdAndPeriode(
            Long inscriptionId,
            String periode
    );

    List<EvaluationConduite>
    findByInscriptionClasseIdAndInscriptionAnneeScolaireIdAndPeriode(
            Long classeId,
            Long anneeScolaireId,
            String periode
    );
}
