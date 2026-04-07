package com.saas.school.repository;

import com.saas.school.entity.AnneeScolaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnneeScolaireRepository extends JpaRepository<AnneeScolaire, Long> {

    // vérifier doublon
    boolean existsByNomAndEcoleId(String nom, Long ecoleId);

    // récupérer par école
    List<AnneeScolaire> findByEcoleId(Long ecoleId);

    // année active
    Optional<AnneeScolaire> findByEcoleIdAndActiveTrue(Long ecoleId);
    @Modifying
    @Query("UPDATE AnneeScolaire a SET a.active = false WHERE a.ecole.id = :ecoleId")
    void desactiverToutes(@Param("ecoleId") Long ecoleId);
}