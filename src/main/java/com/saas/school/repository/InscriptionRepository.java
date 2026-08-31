package com.saas.school.repository;

import com.saas.school.entity.Classe;
import com.saas.school.entity.Inscription;
import com.saas.school.service.StatutInscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InscriptionRepository extends JpaRepository<Inscription, Long> {

    boolean existsByEleveIdAndAnneeScolaireId(Long eleveId, Long anneeId);

    List<Inscription> findByEleveId(Long eleveId);

    Optional<Inscription> findByEleveIdAndAnneeScolaireId(
            Long eleveId,
            Long anneeId
    );

    List<Inscription> findByClasseIdAndAnneeScolaire_Id(Long classeId, Long anneeId);

    List<Inscription> findByEcoleIdAndAnneeScolaire_Id(Long ecoleId, Long anneeId);

    Collection<Inscription> findByEcoleId(Long ecoleId);

    List<Inscription> findByEcoleIdAndAnneeScolaireId(
            Long ecoleId,
            Long anneeId
    );

    @Query("""
            SELECT i
            FROM Inscription i
            WHERE i.classe.id = :classeId
            AND i.anneeScolaire.active = true
            AND i.statut = com.saas.school.service.StatutInscription.VALIDE
            """)
    List<Inscription> findElevesActifsClasse(Long classeId);


    List<Inscription> findByClasseIdAndAnneeScolaireId(Long classeId, Long anneeId);

    boolean existsByEleveIdAndAnneeScolaireIdAndStatut(Long eleveId, Long id, StatutInscription statutInscription);

    List<Inscription> findByClasse_NiveauIdAndAnneeScolaireId(Long niveauId, Long anneeId);


    List<Inscription> findByClasseIdAndAnneeScolaire_ActiveTrue(Long id);


    boolean existsByEleveIdAndClasseIdAndAnneeScolaire_ActiveTrue(Long eleveId, Long id);

    Optional<Inscription> findFirstByEleveIdOrderByIdDesc(Long eleveId);

    boolean existsByEleveIdAndClasseIdAndAnneeScolaireId(Long eleveId, Long id, Long id1);


    List<Inscription> findByEcole_IdAndAnneeScolaire_ActiveTrue(Long ecoleId);
}