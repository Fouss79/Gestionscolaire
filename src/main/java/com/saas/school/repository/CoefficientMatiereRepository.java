package com.saas.school.repository;

import com.saas.school.entity.CoefficientMatiere;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CoefficientMatiereRepository extends JpaRepository<CoefficientMatiere, Long> {

    List<CoefficientMatiere> findByEcoleId(Long ecoleId);

    List<CoefficientMatiere> findByEcoleIdAndAnneeScolaireId(Long ecoleId, Long anneeScolaireId);

    Optional<CoefficientMatiere> findByEcoleIdAndMatiereIdAndNiveauIdAndSerieIdAndAnneeScolaireId(
            Long ecoleId, Long matiereId, Long niveauId, Long serieId, Long anneeScolaireId
    );

    Optional<CoefficientMatiere> findByEcoleIdAndMatiereIdAndNiveauIdAndSerieIsNullAndAnneeScolaireId(
            Long ecoleId, Long matiereId, Long niveauId, Long anneeScolaireId
    );

    @Query("""
        SELECT c FROM CoefficientMatiere c
        WHERE c.ecole.id = :ecoleId
        AND c.matiere.id = :matiereId
        AND c.niveau.id = :niveauId
        AND c.anneeScolaire.id = :anneeScolaireId
        AND (:serieId IS NULL OR c.serie.id = :serieId OR c.serie IS NULL)
        ORDER BY CASE WHEN c.serie IS NULL THEN 1 ELSE 0 END
        """)
    List<CoefficientMatiere> findCandidats(
            @Param("ecoleId") Long ecoleId,
            @Param("matiereId") Long matiereId,
            @Param("niveauId") Long niveauId,
            @Param("serieId") Long serieId,
            @Param("anneeScolaireId") Long anneeScolaireId
    );
    List<CoefficientMatiere> findByEcoleIdAndAnneeScolaireIdAndNiveauId(Long ecoleId, Long anneeScolaireId, Long niveauId);



    Optional<CoefficientMatiere>
    findByEcoleIdAndMatiereIdAndNiveauIdAndSerieIdAndAnneeScolaireIdAndSousGroupeId(
            Long ecoleId,
            Long matiereId,
            Long niveauId,
            Long serieId,
            Long anneeScolaireId,
            Long sousGroupeId
    );

    Optional<CoefficientMatiere>
    findByEcoleIdAndMatiereIdAndNiveauIdAndSerieIdAndAnneeScolaireIdAndSousGroupeIsNull(
            Long ecoleId,
            Long matiereId,
            Long niveauId,
            Long serieId,
            Long anneeScolaireId
    );
    Optional<CoefficientMatiere>
    findByEcoleIdAndMatiereIdAndNiveauIdAndSerieIsNullAndAnneeScolaireIdAndSousGroupeIsNull(
            Long ecoleId,
            Long matiereId,
            Long niveauId,
            Long anneeScolaireId
    );
    @Query("""
    SELECT c
    FROM CoefficientMatiere c
    WHERE c.ecole.id = :ecoleId
      AND c.matiere.id = :matiereId
      AND c.niveau.id = :niveauId
      AND c.anneeScolaire.id = :anneeScolaireId
      AND (
            (:serieId IS NULL AND c.serie IS NULL)
            OR
            (:serieId IS NOT NULL AND c.serie.id = :serieId)
          )
      AND (
            (:classeId IS NULL AND c.classe IS NULL)
            OR
            (:classeId IS NOT NULL AND c.classe.id = :classeId)
          )
      AND (
            (:sousGroupeId IS NULL AND c.sousGroupe IS NULL)
            OR
            (:sousGroupeId IS NOT NULL AND c.sousGroupe.id = :sousGroupeId)
          )
""")
    Optional<CoefficientMatiere> findCoefficient(
            @Param("ecoleId") Long ecoleId,
            @Param("matiereId") Long matiereId,
            @Param("niveauId") Long niveauId,
            @Param("serieId") Long serieId,
            @Param("anneeScolaireId") Long anneeScolaireId,
            @Param("classeId") Long classeId,
            @Param("sousGroupeId") Long sousGroupeId
    );

    Optional<CoefficientMatiere>
    findByMatiereIdAndNiveauIdAndAnneeScolaireIdAndClasseIdAndSousGroupeId(
            Long matiereId,
            Long niveauId,
            Long anneeId,
            Long classeId,
            Long sousGroupeId
    );
}