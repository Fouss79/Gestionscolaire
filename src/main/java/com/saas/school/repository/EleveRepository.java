package com.saas.school.repository;

import com.saas.school.entity.Eleve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EleveRepository extends JpaRepository<Eleve, Long> {

    List<Eleve> findByClasseId(Long classeId);

    List<Eleve> findByEcoleId(Long ecoleId);
}
