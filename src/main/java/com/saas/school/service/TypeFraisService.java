package com.saas.school.service;

import com.saas.school.entity.Ecole;
import com.saas.school.entity.TypeFrais;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.TypeFraisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TypeFraisService {

    private final TypeFraisRepository typeFraisRepository;
    private final EcoleRepository ecoleRepository;
    private final LigneFraisService ligneFraisService;

    public TypeFrais creer(Long ecoleId, TypeFrais typeFrais) {

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() ->
                        new RuntimeException("École introuvable"));

        typeFrais.setEcole(ecole);

        TypeFrais saved = typeFraisRepository.save(typeFrais);

        // Rattrapage : applique immédiatement ce nouveau type de frais
        // aux inscriptions déjà existantes (qui ne l'ont pas encore).
        ligneFraisService.appliquerTypeFraisAuxInscriptionsExistantes(saved.getId());

        return saved;
    }

    public List<TypeFrais> getByEcole(Long ecoleId) {
        return typeFraisRepository.findByEcoleId(ecoleId);
    }

    public TypeFrais getById(Long id) {
        return typeFraisRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Type de frais introuvable"));
    }

    public void supprimer(Long id) {
        typeFraisRepository.deleteById(id);
    }
}