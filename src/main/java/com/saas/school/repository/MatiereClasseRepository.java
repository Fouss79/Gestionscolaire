package com.saas.school.repository;

import com.saas.school.entity.MatiereClasse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatiereClasseRepository extends JpaRepository<MatiereClasse, Long> {
    List<MatiereClasse> findByClasseIdAndAnneeScolaireId(Long classeId, Long anneeId);

    List<MatiereClasse> findByClasseId(Long classeId);
    Optional<MatiereClasse> findByMatiereIdAndClasseIdAndAnneeScolaireId(
            Long matiereId,
            Long classeId,
            Long anneeId
    );
    boolean existsByMatiereIdAndClasseIdAndAnneeScolaireIdAndEcoleId(
            Long matiereId,
            Long classeId,
            Long anneeId,
            Long ecoleId
    );
    boolean existsByMatiereIdAndClasseIdAndEcoleId(
            Long matiereId,
            Long classeId,
            Long ecoleId
    );



}
