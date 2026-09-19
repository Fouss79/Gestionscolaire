package com.saas.school.repository;

import com.saas.school.entity.EpreuveExamen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EpreuveExamenRepository extends JpaRepository<EpreuveExamen, Long> {

    List<EpreuveExamen> findByExamenId(Long examenId);

    List<EpreuveExamen> findByCreneauId(Long creneauId);

    // Couples (niveau, série) distincts couverts par les épreuves d'un examen.
    // Utilisé par le service de répartition (Option B : uniquement les élèves concernés par au moins une épreuve).
    @Query("""
            SELECT DISTINCT cm.niveau.id, cm.serie.id
            FROM EpreuveExamen ee
            JOIN ee.coefficientMatiere cm
            WHERE ee.examen.id = :examenId
            """)
    List<Object[]> findDistinctNiveauSerieByExamenId(@Param("examenId") Long examenId);
}