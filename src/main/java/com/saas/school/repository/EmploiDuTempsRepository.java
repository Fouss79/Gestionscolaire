package com.saas.school.repository;


import com.saas.school.entity.EmploiDuTemps;
import com.saas.school.entity.Presence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmploiDuTempsRepository extends JpaRepository<EmploiDuTemps, Long> {
    void deleteByAnneeScolaireId(Long anneeId);

    List<EmploiDuTemps> findByClasseIdAndAnneeScolaireId(Long classeId, Long anneeId);
    boolean existsByEnseignantIdAndJourAndHeureAndAnneeScolaireId(
            Long enseignantId,
            String jour,
            String heure,
            Long anneeId
    );
    List<EmploiDuTemps> findByClasseIdAndMatiereIdAndJour(
            Long classeId,
            Long matiereId,
            String jour
    );
    List<EmploiDuTemps> findByClasseIdAndAnneeScolaireIdAndJour(
            Long classeId,
            Long anneeId,
            String jour
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
    List<EmploiDuTemps> findByJourAndClasseId(String jour, Long classeId);

    @Query("""
    SELECT e FROM EmploiDuTemps e
    WHERE (:classeId IS NULL OR e.classe.id = :classeId)
    AND (:anneeId IS NULL OR e.anneeScolaire.id = :anneeId)
    AND (:jour IS NULL OR e.jour = :jour)
""")
    List<EmploiDuTemps> filtre(
            @Param("classeId") Long classeId,
            @Param("anneeId") Long anneeId,
            @Param("jour") String jour
    );

    List<EmploiDuTemps> findByJourAndAnneeScolaireId(String jour, Long anneeId);
    List<EmploiDuTemps> findByClasseIdAndJour(Long classeId, String jour);

}