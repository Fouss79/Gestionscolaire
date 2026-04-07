package com.saas.school.repository;

import com.saas.school.entity.Classe;
import com.saas.school.entity.Inscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InscriptionRepository extends JpaRepository<Inscription, Long> {

boolean existsByEleveIdAndAnneeScolaireId(Long eleveId, Long anneeId);

    List<Inscription> findByEleveId(Long eleveId);
    Inscription findByEleveIdAndAnneeScolaireId(Long eleveId, Long anneeId);

    List<Inscription> findByClasseIdAndAnneeScolaire_Id(Long classeId, Long anneeId);
    List<Inscription> findByEcoleIdAndAnneeScolaire_Id(Long ecoleId, Long anneeId);
}