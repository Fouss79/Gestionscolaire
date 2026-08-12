package com.saas.school.service;


import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Niveau;
import com.saas.school.entity.TypeFrais;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EcoleService {

    private final EcoleRepository ecoleRepository;
    private final NiveauRepository niveauRepository;
    private final TypeFraisRepository typeFraisRepository;
    private final TarifRepository tarifRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;

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

    public boolean tousLesTarifsSontConfigures(Long ecoleId) {

        List<Niveau> niveaux = niveauRepository.findByEcoleId(ecoleId);
        List<TypeFrais> typesFrais = typeFraisRepository.findByEcoleId(ecoleId);

        if (niveaux.isEmpty()) {
            return true; // pas encore de niveau créé → rien à signaler pour l'instant
        }

        AnneeScolaire anneeActive = anneeScolaireRepository
                .findByEcoleIdAndActiveTrue(ecoleId)
                .orElse(null);

        if (anneeActive == null) return false;

        for (Niveau niveau : niveaux) {
            for (TypeFrais type : typesFrais) {
                boolean existe = tarifRepository
                        .findByNiveauIdAndAnneeScolaireIdAndTypeFrais_Code(
                                niveau.getId(), anneeActive.getId(), type.getCode()
                        )
                        .isPresent();

                if (!existe) return false;
            }
        }

        return true;
    }

}
