package com.saas.school.repository;

import com.saas.school.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour la gestion des notes
 * Gère les requêtes complexes pour les notes, moyennes et statistiques
 */
@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    // =========================================================
    // 📚 NOTES PAR CLASSE ET ANNÉE
    // =========================================================

    /**
     * Récupère toutes les notes d'une classe pour une année donnée
     */
    List<Note> findByClasseIdAndAnneeScolaireId(
            @Param("classeId") Long classeId,
            @Param("anneeId") Long anneeId
    );

    /**
     * Récupère les notes d'un élève spécifique dans une classe pour une année donnée
     */
    List<Note> findByClasseIdAndAnneeScolaireIdAndEleveId(
            @Param("classeId") Long classeId,
            @Param("anneeId") Long anneeId,
            @Param("eleveId") Long eleveId
    );

    /**
     * Récupère les notes d'une classe pour une période spécifique
     */
    List<Note> findByClasseIdAndAnneeScolaireIdAndPeriode(
            @Param("classeId") Long classeId,
            @Param("anneeId") Long anneeId,
            @Param("periode") String periode
    );

    /**
     * Récupère les notes d'un élève dans une classe pour une période spécifique
     */
    List<Note> findByClasseIdAndAnneeScolaireIdAndEleveIdAndPeriode(
            @Param("classeId") Long classeId,
            @Param("anneeId") Long anneeId,
            @Param("eleveId") Long eleveId,
            @Param("periode") String periode
    );

    // =========================================================
    // 🔎 RECHERCHE PAR ÉLÈVE
    // =========================================================

    /**
     * Récupère toutes les notes d'un élève
     */
    List<Note> findByEleveId(Long eleveId);

    /**
     * Récupère les notes d'un élève pour une classe et année spécifiques
     */
    List<Note> findByEleveIdAndClasseIdAndAnneeScolaireId(
            @Param("eleveId") Long eleveId,
            @Param("classeId") Long classeId,
            @Param("anneeId") Long anneeId
    );

    /**
     * Récupère toutes les notes d'un élève pour une année scolaire donnée
     */
    List<Note> findByEleveIdAndAnneeScolaireId(
            @Param("eleveId") Long eleveId,
            @Param("anneeScolaireId") Long anneeScolaireId
    );

    // =========================================================
    // 📚 RECHERCHE PAR INSCRIPTION
    // =========================================================

    /**
     * Récupère les notes d'une inscription pour une période donnée
     */


    /**
     * Récupère une note spécifique par inscription, coefficient et période
     */
    Optional<Note> findByInscriptionIdAndCoefficientMatiereIdAndPeriode(
            @Param("inscriptionId") Long inscriptionId,
            @Param("coefficientMatiereId") Long coefficientMatiereId,
            @Param("periode") String periode
    );

    /**
     * Récupère les notes d'une inscription pour une année donnée
     */


    /**
     * Récupère les notes d'une inscription pour une année donnée (alias)
     */
    List<Note> findByInscriptionIdAndAnneeScolaireId(
            @Param("inscriptionId") Long inscriptionId,
            @Param("anneeScolaireId") Long anneeScolaireId
    );

    // =========================================================
    // 📝 RECHERCHE PAR MATIÈRE
    // =========================================================

    /**
     * Récupère les notes d'une classe pour une matière, année et période données
     */
    List<Note> findByClasseIdAndMatiereIdAndAnneeScolaireIdAndPeriode(
            @Param("classeId") Long classeId,
            @Param("matiereId") Long matiereId,
            @Param("anneeScolaireId") Long anneeScolaireId,
            @Param("periode") String periode
    );

    /**
     * Récupère les notes d'une classe pour un coefficient matière et période donnés
     */
    List<Note> findByInscription_Classe_IdAndCoefficientMatiere_IdAndPeriode(
            @Param("classeId") Long classeId,
            @Param("coefficientMatiereId") Long coefficientMatiereId,
            @Param("periode") String periode
    );

    /**
     * Récupère une note par élève, matière, classe, année et période
     */
    Optional<Note> findByEleveAndMatiereAndClasseAndAnneeScolaireAndPeriode(
            @Param("eleve") Eleve eleve,
            @Param("matiere") Matiere matiere,
            @Param("classe") Classe classe,
            @Param("annee") AnneeScolaire annee,
            @Param("periode") String periode
    );

    // =========================================================
    // 🔎 EXISTENCE D'UNE NOTE
    // =========================================================

    /**
     * Vérifie si une note existe pour un élève, matière, période, classe et année
     */
    boolean existsByEleveIdAndMatiereIdAndPeriodeAndClasseIdAndAnneeScolaireId(
            @Param("eleveId") Long eleveId,
            @Param("matiereId") Long matiereId,
            @Param("periode") String periode,
            @Param("classeId") Long classeId,
            @Param("anneeId") Long anneeId
    );

    /**
     * Vérifie si une note existe (version objet)
     */
    boolean existsByEleveAndMatiereAndClasseAndAnneeScolaireAndPeriode(
            @Param("eleve") Eleve eleve,
            @Param("matiere") Matiere matiere,
            @Param("classe") Classe classe,
            @Param("anneeScolaire") AnneeScolaire anneeScolaire,
            @Param("periode") String periode
    );

    // =========================================================
    // 📊 STATISTIQUES ET MOYENNES
    // =========================================================

    /**
     * Calcule les moyennes de tous les élèves d'une classe pour une période donnée
     *
     * Règles de calcul :
     * - Note = (Contrôle + Examen × 2) / 3
     * - Moyenne = Σ(Note × Coefficient) / Σ(Coefficient)
     * - Seuls les coefficients compatibles avec les sous-groupes sont pris en compte
     *
     * @return Liste d'objets [eleveId, moyenne]
     */
    @Query("""
        SELECT n.eleve.id,
               CAST(
                   SUM(
                       (
                           (COALESCE(n.nClass, 0) +
                            COALESCE(n.nExem, 0) * 2) / 3.0
                       ) * n.coeff
                   ) / NULLIF(SUM(n.coeff), 0)
               AS double)
        FROM Note n
        WHERE n.classe.id = :classeId
          AND n.anneeScolaire.id = :anneeId
          AND n.periode = :periode
          AND (
                n.coefficientMatiere.sousGroupe IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM Eleve e
                    JOIN e.sousGroupes sg
                    WHERE e.id = n.eleve.id
                      AND sg.id = n.coefficientMatiere.sousGroupe.id
                )
          )
        GROUP BY n.eleve.id
        HAVING SUM(n.coeff) > 0
        ORDER BY 
            SUM(
                (
                    (COALESCE(n.nClass, 0) +
                     COALESCE(n.nExem, 0) * 2) / 3.0
                ) * n.coeff
            ) / NULLIF(SUM(n.coeff), 0) DESC
    """)
    List<Object[]> calculerMoyennesClasse(
            @Param("classeId") Long classeId,
            @Param("anneeId") Long anneeId,
            @Param("periode") String periode
    );

    /**
     * Calcule la moyenne d'une classe pour une matière spécifique
     */
    @Query("""
        SELECT AVG(
            (COALESCE(n.nClass, 0) + COALESCE(n.nExem, 0) * 2) / 3.0
        )
        FROM Note n
        WHERE n.classe.id = :classeId
          AND n.matiere.id = :matiereId
          AND n.anneeScolaire.id = :anneeId
          AND n.periode = :periode
          AND (
                n.coefficientMatiere.sousGroupe IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM Eleve e
                    JOIN e.sousGroupes sg
                    WHERE e.id = n.eleve.id
                      AND sg.id = n.coefficientMatiere.sousGroupe.id
                )
          )
    """)
    Double calculerMoyenneClasseParMatiere(
            @Param("classeId") Long classeId,
            @Param("matiereId") Long matiereId,
            @Param("anneeId") Long anneeId,
            @Param("periode") String periode
    );

    /**
     * Récupère les meilleures notes d'une classe pour une période donnée
     */
    @Query("""
        SELECT n.eleve.id,
               n.eleve.nom,
               n.eleve.prenom,
               MAX(
                   (COALESCE(n.nClass, 0) + COALESCE(n.nExem, 0) * 2) / 3.0
               ) as meilleureNote
        FROM Note n
        WHERE n.classe.id = :classeId
          AND n.anneeScolaire.id = :anneeId
          AND n.periode = :periode
        GROUP BY n.eleve.id, n.eleve.nom, n.eleve.prenom
        ORDER BY meilleureNote DESC
    """)
    List<Object[]> trouverMeilleuresNotesClasse(
            @Param("classeId") Long classeId,
            @Param("anneeId") Long anneeId,
            @Param("periode") String periode
    );

    // =========================================================
    // 👨‍🎓 STATISTIQUES ÉLÈVES
    // =========================================================

    /**
     * Compte le nombre d'élèves distincts dans une classe pour une année donnée
     */
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

    /**
     * Récupère le nombre de notes par matière pour une classe
     */
    @Query("""
        SELECT n.matiere.nom,
               COUNT(n.id) as nombreNotes,
               AVG(
                   (COALESCE(n.nClass, 0) + COALESCE(n.nExem, 0) * 2) / 3.0
               ) as moyenneMatiere
        FROM Note n
        WHERE n.classe.id = :classeId
          AND n.anneeScolaire.id = :anneeId
          AND n.periode = :periode
        GROUP BY n.matiere.nom
        ORDER BY moyenneMatiere DESC
    """)
    List<Object[]> getStatistiquesParMatiere(
            @Param("classeId") Long classeId,
            @Param("anneeId") Long anneeId,
            @Param("periode") String periode
    );

    /**
     * Récupère la moyenne générale d'un élève pour une période donnée
     */
    @Query("""
        SELECT CAST(
                   SUM(
                       (
                           (COALESCE(n.nClass, 0) +
                            COALESCE(n.nExem, 0) * 2) / 3.0
                       ) * n.coeff
                   ) / NULLIF(SUM(n.coeff), 0)
               AS double)
        FROM Note n
        WHERE n.eleve.id = :eleveId
          AND n.anneeScolaire.id = :anneeId
          AND n.periode = :periode
          AND (
                n.coefficientMatiere.sousGroupe IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM Eleve e
                    JOIN e.sousGroupes sg
                    WHERE e.id = n.eleve.id
                      AND sg.id = n.coefficientMatiere.sousGroupe.id
                )
          )
    """)
    Double calculerMoyenneEleveParPeriode(
            @Param("eleveId") Long eleveId,
            @Param("anneeId") Long anneeId,
            @Param("periode") String periode
    );

    // =========================================================
    // 🗑️ SUPPRESSION EN MASSE
    // =========================================================

    /**
     * Supprime toutes les notes d'un élève
     */
    void deleteByEleveId(Long eleveId);

    /**
     * Supprime toutes les notes d'une classe pour une année donnée
     */
    void deleteByClasseIdAndAnneeScolaireId(Long classeId, Long anneeId);

    /**
     * Supprime toutes les notes d'une période donnée
     */
    void deleteByPeriode(String periode);

    // =========================================================
    // 📊 RAPPORTS ET ANALYSES
    // =========================================================

    /**
     * Récupère les notes d'un élève par période
     */
    @Query("""
        SELECT n.periode,
               n.matiere.nom,
               n.nClass,
               n.nExem,
               (COALESCE(n.nClass, 0) + COALESCE(n.nExem, 0) * 2) / 3.0 as moyenne,
               n.coeff
        FROM Note n
        WHERE n.eleve.id = :eleveId
          AND n.anneeScolaire.id = :anneeId
        ORDER BY n.periode, n.matiere.nom
    """)
    List<Object[]> getBulletinEleve(
            @Param("eleveId") Long eleveId,
            @Param("anneeId") Long anneeId
    );







        // Notes de toute la classe pour une matière SANS sous-groupe (matières communes)
        List<Note> findByInscription_Classe_IdAndCoefficientMatiere_IdAndPeriodeAndSousGroupeIsNull(
                Long classeId, Long coefficientMatiereId, String periode
        );

        // Notes d'un sous-groupe précis (LV2, TP...)
        List<Note> findBySousGroupe_IdAndCoefficientMatiere_IdAndPeriode(
                Long sousGroupeId, Long coefficientMatiereId, String periode
        );

        Optional<Note> findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeId(
                Long inscriptionId, Long coefficientMatiereId, String periode, Long sousGroupeId
        );

        Optional<Note> findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeIsNull(
                Long inscriptionId, Long coefficientMatiereId, String periode
        );

        List<Note> findByInscriptionIdAndPeriode(Long inscriptionId, String periode);

        List<Note> findByInscriptionIdAndInscription_AnneeScolaireId(Long inscriptionId, Long anneeScolaireId);

    List<Note> findByInscription_Classe_IdAndCoefficientMatiere_IdAndPeriodeAndSousGroupe_Id(Long classeId, Long coefficientMatiereId, String periode, Long sousGroupeId);


    @Query("""
    SELECT n
    FROM Note n
    JOIN n.inscription i
    WHERE i.classe.id = :classeId
      AND n.coefficientMatiere.id = :coefficientMatiereId
      AND n.periode = :periode
      AND n.sousGroupe.id = :sousGroupeId
""")
    List<Note> findNotesClasseMatierePeriodeSousGroupe(
            @Param("classeId") Long classeId,
            @Param("coefficientMatiereId") Long coefficientMatiereId,
            @Param("periode") String periode,
            @Param("sousGroupeId") Long sousGroupeId
    );

    @Query("""
    SELECT n
    FROM Note n
    JOIN n.inscription i
    WHERE i.classe.id = :classeId
      AND n.coefficientMatiere.id = :coefficientMatiereId
      AND n.periode = :periode
      AND n.sousGroupe IS NULL
""")
    List<Note> findNotesClasseMatierePeriodeSansSousGroupe(
            @Param("classeId") Long classeId,
            @Param("coefficientMatiereId") Long coefficientMatiereId,
            @Param("periode") String periode
    );


}