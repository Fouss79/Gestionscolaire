package com.saas.school.repository;

import com.saas.school.entity.EmploiDuTemps;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmploiDuTempsRepository
        extends JpaRepository<EmploiDuTemps, Long> {

    // =========================================================
    // 🗑️ SUPPRESSION PAR ANNÉE
    // =========================================================

    void deleteByAnneeScolaireId(Long anneeId);


    // =========================================================
    // 📋 CONSULTATION
    // =========================================================

    List<EmploiDuTemps> findByClasseIdAndAnneeScolaireId(
            Long classeId,
            Long anneeId
    );


    List<EmploiDuTemps> findByClasseIdAndMatiereIdAndJour(
            Long classeId,
            Long matiereId,
            String jour
    );




    List<EmploiDuTemps> findByJourAndClasseId(
            String jour,
            Long classeId
    );


    List<EmploiDuTemps> findByJourAndAnneeScolaireId(
            String jour,
            Long anneeId
    );


    List<EmploiDuTemps> findByClasseIdAndJour(
            Long classeId,
            String jour
    );


    // =========================================================
    // 🔎 FILTRE
    // =========================================================

    @Query("""
        SELECT e
        FROM EmploiDuTemps e
        WHERE (:classeId IS NULL OR e.classe.id = :classeId)
          AND (:anneeId IS NULL OR e.anneeScolaire.id = :anneeId)
          AND (:jour IS NULL OR e.jour = :jour)
    """)
    List<EmploiDuTemps> filtre(
            @Param("classeId") Long classeId,
            @Param("anneeId") Long anneeId,
            @Param("jour") String jour
    );


    // =========================================================
    // 📚 CLASSE ACTIVE
    // =========================================================

    @Query("""
        SELECT e
        FROM EmploiDuTemps e
        WHERE e.classe.id = :classeId
          AND e.anneeScolaire.active = true
          AND e.jour = :jour
    """)
    List<EmploiDuTemps> findClasseActive(
            @Param("classeId") Long classeId,
            @Param("jour") String jour
    );


    // =========================================================
    // ⏱️ HEURES DÉJÀ PLANIFIÉES
    // =========================================================

    /*
     * Cours généraux de la classe.
     *
     * On compte uniquement les créneaux dont
     * sousGroupe est NULL.
     */
    @Query("""
        SELECT COALESCE(SUM(e.heureFin - e.heureDebut), 0)
        FROM EmploiDuTemps e
        WHERE e.classe.id = :classeId
          AND e.matiere.id = :matiereId
          AND e.anneeScolaire.id = :anneeId
          AND e.sousGroupe IS NULL
    """)
    Integer totalHeuresDejaPlanifiees(
            @Param("classeId") Long classeId,
            @Param("matiereId") Long matiereId,
            @Param("anneeId") Long anneeId
    );


    /*
     * Cours spécifiques à un sous-groupe.
     */
    @Query("""
        SELECT COALESCE(SUM(e.heureFin - e.heureDebut), 0)
        FROM EmploiDuTemps e
        WHERE e.classe.id = :classeId
          AND e.matiere.id = :matiereId
          AND e.anneeScolaire.id = :anneeId
          AND e.sousGroupe.id = :sousGroupeId
    """)
    Integer totalHeuresDejaPlanifieesSousGroupe(
            @Param("classeId") Long classeId,
            @Param("matiereId") Long matiereId,
            @Param("anneeId") Long anneeId,
            @Param("sousGroupeId") Long sousGroupeId
    );


    // =========================================================
    // 👨‍🏫 CONFLIT ENSEIGNANT
    // =========================================================

    @Query("""
        SELECT COUNT(e) > 0
        FROM EmploiDuTemps e
        WHERE e.id <> :id
          AND e.enseignant.id = :enseignantId
          AND e.anneeScolaire.id = :anneeId
          AND e.jour = :jour
          AND e.heureDebut < :fin
          AND e.heureFin > :debut
    """)
    boolean existsConflitEnseignant(
            @Param("id") Long id,
            @Param("enseignantId") Long enseignantId,
            @Param("anneeId") Long anneeId,
            @Param("jour") String jour,
            @Param("fin") int fin,
            @Param("debut") int debut
    );


    // =========================================================
    // 👨‍🎓 CONFLIT CLASSE + SOUS-GROUPE
    // =========================================================

    /*
     * Règles :
     *
     * 1. Sous-groupe 1 vs Sous-groupe 1
     *    → CONFLIT
     *
     * 2. Sous-groupe 1 vs Sous-groupe 2
     *    → PAS DE CONFLIT
     *
     * 3. Sous-groupe 1 vs NULL
     *    → CONFLIT
     *
     * 4. NULL vs Sous-groupe 2
     *    → CONFLIT
     *
     * 5. NULL vs NULL
     *    → CONFLIT
     */
    @Query("""
        SELECT COUNT(e) > 0
        FROM EmploiDuTemps e
        WHERE e.id <> :id
          AND e.classe.id = :classeId
          AND e.anneeScolaire.id = :anneeId
          AND e.jour = :jour
          AND e.heureDebut < :fin
          AND e.heureFin > :debut
          AND (
                e.sousGroupe IS NULL
                OR :sousGroupeId IS NULL
                OR e.sousGroupe.id = :sousGroupeId
          )
    """)
    boolean existsConflitClasseAvecSousGroupe(
            @Param("id") Long id,
            @Param("classeId") Long classeId,
            @Param("sousGroupeId") Long sousGroupeId,
            @Param("anneeId") Long anneeId,
            @Param("jour") String jour,
            @Param("fin") int fin,
            @Param("debut") int debut
    );


    // =========================================================
    // 🏫 CONFLIT SALLE
    // =========================================================

    @Query("""
        SELECT COUNT(e) > 0
        FROM EmploiDuTemps e
        WHERE e.id <> :id
          AND e.salle.id = :salleId
          AND e.anneeScolaire.id = :anneeId
          AND e.jour = :jour
          AND e.heureDebut < :fin
          AND e.heureFin > :debut
    """)
    boolean existsConflitSalle(
            @Param("id") Long id,
            @Param("salleId") Long salleId,
            @Param("anneeId") Long anneeId,
            @Param("jour") String jour,
            @Param("fin") int fin,
            @Param("debut") int debut
    );


    // =========================================================
    // 🏫 SALLE - GÉNÉRATION AUTOMATIQUE
    // =========================================================

    boolean existsBySalleIdAndAnneeScolaireIdAndJourAndHeureDebutLessThanAndHeureFinGreaterThan(
            Long salleId,
            Long anneeScolaireId,
            String jour,
            int heureFin,
            int heureDebut
    );


    // =========================================================
    // 👨‍🏫 PROF - GÉNÉRATION AUTOMATIQUE
    // =========================================================

    boolean existsByEnseignantIdAndAnneeScolaireIdAndJourAndHeureDebutLessThanAndHeureFinGreaterThan(
            Long enseignantId,
            Long anneeScolaireId,
            String jour,
            Integer heureFin,
            Integer heureDebut
    );


    // =========================================================
    // 👨‍🎓 CLASSE - ANCIENNE MÉTHODE
    // =========================================================
    /*
     * Tu peux la conserver si d'autres parties de ton application
     * l'utilisent.
     *
     * Mais pour la création/modification avec sous-groupe,
     * utilise plutôt existsConflitClasseAvecSousGroupe().
     */



    // =========================================================
    // 🔎 ANCIENNE RECHERCHE PAR "HEURE"
    // =========================================================
    /*
     * À conserver uniquement si elle est encore utilisée ailleurs.
     *
     * Attention : ton entité possède maintenant heureDebut/heureFin.
     * Si le champ "heure" a été supprimé de EmploiDuTemps,
     * cette méthode doit être supprimée.
     */

}