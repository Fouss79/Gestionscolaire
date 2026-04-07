package com.saas.school.service;


import com.saas.school.entity.Ecole;
import com.saas.school.repository.EcoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EcoleService {

    private final EcoleRepository ecoleRepository;

    public Ecole creerEcole(Ecole ecole) {
        ecole.setCreatedAt(LocalDateTime.now());
        ecole.setActive(true);
        return ecoleRepository.save(ecole);
    }

    public Ecole getById(Long id) {
        return ecoleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ecole introuvable"));
    }
}
