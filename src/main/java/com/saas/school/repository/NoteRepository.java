package com.saas.school.repository;

import com.saas.school.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByClasseIdAndAnneeScolaireId(Long classeId, Long anneeId);

    List<Note> findByClasseIdAndAnneeScolaireIdAndEleveId(
            Long classeId, Long anneeId, Long eleveId);

    List<Note> findByClasseIdAndAnneeScolaireIdAndPeriode(
            Long classeId, Long anneeId, String periode);
    List<Note> findByClasseIdAndAnneeScolaireIdAndEleveIdAndPeriode (Long classeId, Long anneeId, Long eleveId,String periode);

    boolean existsByEleveIdAndMatiereIdAndPeriodeAndClasseIdAndAnneeScolaireId(
            Long eleveId,
            Long matiereId,
            String periode,
            Long classeId,
            Long anneeId
    );
    @Query("""
    SELECT n.eleve.id,
           SUM(((n.nClass + n.nExem*2)/3) * n.coeff) / SUM(n.coeff)
    FROM Note n
    WHERE n.classe.id = :classeId
      AND n.anneeScolaire.id = :anneeId
      AND n.periode = :periode
    GROUP BY n.eleve.id
    ORDER BY SUM(((n.nClass + n.nExem*2)/3) * n.coeff) / SUM(n.coeff) DESC
""")
    List<Object[]> calculerMoyennesClasse(
            @Param("classeId") Long classeId,
            @Param("anneeId") Long anneeId,
            @Param("periode") String periode
    );
    @Query("""
    SELECT COUNT(DISTINCT n.eleve.id)
    FROM Note n
    WHERE n.classe.id = :classeId
    AND n.anneeScolaire.id = :anneeId
""")
    int countDistinctEleves(
            @Param("classeId") Long classeId,
            @Param("anneeId") Long anneeId
    );

    boolean existsByEleveAndMatiereAndClasseAndAnneeScolaireAndPeriode(
            Eleve eleve,
            Matiere matiere,
            Classe classe,
            AnneeScolaire anneeScolaire,
            String periode
    );

}
