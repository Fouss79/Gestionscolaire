package com.saas.school.repository;

import com.saas.school.entity.Classe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClasseRepository extends JpaRepository<Classe, Long> {
    long countByEcoleId(Long ecoleId);

        List<Classe> findByEcoleId(Long ecoleId);

        @Query("""
        SELECT COUNT(c) > 0 FROM Classe c
        WHERE c.ecole.id = :ecoleId
        AND c.niveau.id = :niveauId
        AND ((:serieId IS NULL AND c.serie IS NULL) OR c.serie.id = :serieId)
        AND ((:groupeId IS NULL AND c.groupe IS NULL) OR c.groupe.id = :groupeId)
        """)
        boolean existsCombinaison(
                @Param("ecoleId") Long ecoleId,
                @Param("niveauId") Long niveauId,
                @Param("serieId") Long serieId,
                @Param("groupeId") Long groupeId
        );

    List<Classe> findByEcoleIdAndNiveauId(Long ecoleId, Long niveauId);
}

