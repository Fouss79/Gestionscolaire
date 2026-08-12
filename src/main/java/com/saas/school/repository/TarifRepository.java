package com.saas.school.repository;

import com.saas.school.entity.Tarif;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TarifRepository extends JpaRepository<Tarif, Long> {

    Optional<Tarif> findByNiveauIdAndAnneeScolaireIdAndTypeFrais_Code(
            Long niveauId,
            Long anneeId,
            String codeTypeFrais
    );

    List<Tarif> findByEcoleId(Long ecoleId);

    List<Tarif> findByEcoleIdAndAnneeScolaireId(
            Long ecoleId,
            Long anneeId
    );
}