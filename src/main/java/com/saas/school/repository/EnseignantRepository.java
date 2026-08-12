package com.saas.school.repository;

import com.saas.school.entity.Enseignant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnseignantRepository extends JpaRepository<Enseignant, Long> {
    List<Enseignant> findByEcoleId(Long ecoleId);
    long countByEcoleId(Long ecoleId);

    List<Enseignant> findByEcoleIdAndActifTrue(Long ecoleId);

    @Query("""
SELECT e FROM Enseignant e
JOIN e.matieresEnseignees m
WHERE m.id = :matiereId
""")
    List<Enseignant> findByMatiere(@Param("matiereId") Long matiereId);
}