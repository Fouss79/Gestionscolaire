package com.saas.school.service;


import com.saas.school.entity.Ecole;
import com.saas.school.repository.EcoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.List;

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
    // 🔴 activer / désactiver école
    public Ecole toggleActive(Long id) {
        Ecole ecole = ecoleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        ecole.setActive(!ecole.isActive());
        return ecoleRepository.save(ecole);
    }
    // 📋 liste écoles
    public List<Ecole> getAllEcoles() {
        return ecoleRepository.findAll();
    }

}
