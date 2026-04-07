package com.saas.school.repository;

import com.saas.school.entity.Classe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClasseRepository extends JpaRepository<Classe, Long> {

    List<Classe> findByEcoleId(Long ecoleId);
    long countByEcoleId(Long ecoleId);
}