package com.saas.school.repository;

import com.saas.school.entity.Personnel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonnelRepository extends JpaRepository<Personnel, Long> {

    List<Personnel> findByEcoleId(Long ecoleId);

    List<Personnel> findByEcoleIdAndActifTrue(Long ecoleId);

    boolean existsByMatricule(String matricule);

}