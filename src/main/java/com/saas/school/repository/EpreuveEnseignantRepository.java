package com.saas.school.repository;

import com.saas.school.entity.EpreuveEnseignant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EpreuveEnseignantRepository extends JpaRepository<EpreuveEnseignant, Long> {

    List<EpreuveEnseignant> findByEpreuveId(Long epreuveId);

    List<EpreuveEnseignant> findByEnseignantId(Long enseignantId);
}