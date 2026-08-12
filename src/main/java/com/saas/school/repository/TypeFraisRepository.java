package com.saas.school.repository;


import com.saas.school.entity.TypeFrais;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TypeFraisRepository
        extends JpaRepository<TypeFrais, Long> {

    List<TypeFrais> findByEcoleId(Long ecoleId);

    Optional<TypeFrais> findByEcoleIdAndCode(
            Long ecoleId,
            String code
    );
}