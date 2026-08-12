package com.saas.school.repository;

import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Classe;
import com.saas.school.entity.SousGroupe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SousGroupeRepository extends JpaRepository<SousGroupe, Long> {

    List<SousGroupe> findByClasseId(Long classeId);

    Optional<SousGroupe> findByClasseIdAndNom(Long classeId, String nom);

    boolean existsByClasseIdAndNom(Long classeId, String nom);

    boolean existsByClasseIdAndNomAndAnneeScolaireId(Long id, String nom, Long id1);

    List<SousGroupe> findByClasseIdAndAnneeScolaireId(Long id, Long id1);



    // ✅ Ajouter cette méthode
    List<SousGroupe> findByClasseAndAnneeScolaire(Classe classe, AnneeScolaire annee);

    // ✅ Ou alternative avec IDs

}