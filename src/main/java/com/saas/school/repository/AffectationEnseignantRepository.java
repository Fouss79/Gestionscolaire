package com.saas.school.repository;

import com.saas.school.entity.AffectationEnseignant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AffectationEnseignantRepository extends JpaRepository<AffectationEnseignant, Long> {

    List<AffectationEnseignant> findByClasseIdAndCoefficientMatiere_AnneeScolaireId(Long classeId, Long anneeScolaireId);

    List<AffectationEnseignant> findByEnseignantIdAndCoefficientMatiere_AnneeScolaireId(Long enseignantId, Long anneeScolaireId);

    Optional<AffectationEnseignant> findByEnseignantIdAndClasseIdAndCoefficientMatiere_MatiereIdAndCoefficientMatiere_AnneeScolaireId(
            Long enseignantId, Long classeId, Long matiereId, Long anneeScolaireId
    );

    boolean existsByEnseignantIdAndClasseIdAndCoefficientMatiereId(Long enseignantId, Long classeId, Long coefficientMatiereId);

    // 🔥 celle qui manque probablement
    List<AffectationEnseignant> findByCoefficientMatiereId(Long coefficientMatiereId);

}