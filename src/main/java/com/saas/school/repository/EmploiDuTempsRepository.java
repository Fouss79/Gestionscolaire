package com.saas.school.repository;


import com.saas.school.entity.EmploiDuTemps;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EmploiDuTempsRepository extends JpaRepository<EmploiDuTemps, Long> {
    void deleteByAnneeScolaireId(Long anneeId);

    List<EmploiDuTemps> findByClasseIdAndAnneeScolaireId(Long classeId, Long anneeId);
    boolean existsByEnseignantIdAndJourAndHeureAndAnneeScolaireId(
            Long enseignantId,
            String jour,
            String heure,
            Long anneeId
    );
    boolean existsByEnseignantIdAndJourAndHeureDebutLessThanAndHeureFinGreaterThan(
            Long enseignantId,
            String jour,
            int heureFin,
            int heureDebut
    );
    boolean existsByClasseIdAndJourAndHeureDebutLessThanAndHeureFinGreaterThan(Long classeId,String jour,int heureFin, int heureDebut);

    boolean existsByClasseIdAndJourAndMatiereId(
            Long classeId,
            String jour,
            Long matiereId
    );
    List<EmploiDuTemps> findByClasseIdAndMatiereIdAndAnneeScolaireId(
            Long classeId,
            Long matiereId,
            Long anneeId
    );
    @Query("""
SELECT COALESCE(SUM(e.heureFin - e.heureDebut), 0)
FROM EmploiDuTemps e
WHERE e.classe.id = :classeId
AND e.matiere.id = :matiereId
AND e.anneeScolaire.id = :anneeId
""")
    Integer totalHeuresDejaPlanifiees(Long classeId, Long matiereId, Long anneeId);

}