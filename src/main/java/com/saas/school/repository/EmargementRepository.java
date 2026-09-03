package com.saas.school.repository;

import com.saas.school.entity.Emargement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EmargementRepository extends JpaRepository<Emargement, Long> {

    boolean existsByEmploiDuTemps_IdAndDateHeure(Long emploiId, LocalDate date);

    List<Emargement> findByDateHeure(LocalDate date);

    boolean existsByEmploiDuTempsId(Long emploiDuTempsId);

    long countByEmploiDuTempsId(Long emploiDuTempsId);

    void deleteByEmploiDuTempsId(Long emploiDuTempsId);



    // Tous les émargements d'un enseignant
    List<Emargement> findByEmploiDuTemps_Enseignant_Id(Long enseignantId);

    // Émargements d'un enseignant sur une période donnée
    List<Emargement> findByEmploiDuTemps_Enseignant_IdAndDateHeureBetween(
            Long enseignantId, LocalDate debut, LocalDate fin);

    List<Emargement> findByDateHeureBetween(LocalDate debut, LocalDate fin);

    List<Emargement> findByDateHeureBetweenAndEmploiDuTemps_AnneeScolaire_Ecole_Id(
            LocalDate debut, LocalDate fin, Long ecoleId);
    List<Emargement> findByDateHeureBetweenAndEmploiDuTemps_AnneeScolaireId(
            LocalDate debut, LocalDate fin, Long anneeId);

    List<Emargement> findByEmploiDuTemps_Enseignant_IdAndDateHeureBetweenAndEmploiDuTemps_AnneeScolaireId(
            Long enseignantId, LocalDate debut, LocalDate fin, Long anneeId);
}