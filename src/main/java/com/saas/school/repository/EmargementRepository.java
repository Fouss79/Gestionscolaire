package com.saas.school.repository;
import com.saas.school.entity.Emargement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmargementRepository extends JpaRepository<Emargement, Long> {

    List<Emargement> findByJourAndDateHeure(String jour, LocalDate date);

    boolean existsByEnseignant_IdAndClasse_IdAndMatiere_IdAndDateHeure(
            Long enseignantId,
            Long classeId,
            Long matiereId,
            LocalDate date
    );
}