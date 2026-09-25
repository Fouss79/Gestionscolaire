package com.saas.school.service;

import com.saas.school.dto.ConfigurationCreneauxDto;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConfigurationCreneauxService {

    private final ConfigurationCreneauxRepository configRepo;
    private final EcoleRepository ecoleRepo;
    private final CycleRepository cycleRepo;

    private static final Set<String> JOURS = Set.of(
            "LUNDI", "MARDI", "MERCREDI",
            "JEUDI", "VENDREDI", "SAMEDI"
    );

    @Transactional(readOnly = true)
    public ConfigurationCreneauxDto obtenir(
            Long ecoleId, Long cycleId
    ) {
        ConfigurationCreneaux config = configRepo
                .findByEcoleIdAndCycleId(ecoleId, cycleId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Aucune configuration pour ce cycle"
                        )
                );

        return versDto(config);
    }

    @Transactional
    public ConfigurationCreneauxDto enregistrer(
            ConfigurationCreneauxDto dto
    ) {
        if (dto.creneaux() == null ||
                dto.creneaux().isEmpty()) {
            throw new IllegalArgumentException(
                    "Ajoutez au moins un créneau"
            );
        }

        Ecole ecole = ecoleRepo.findById(dto.ecoleId())
                .orElseThrow(() ->
                        new IllegalArgumentException("École introuvable")
                );

        Cycle cycle = cycleRepo.findById(dto.cycleId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Cycle introuvable")
                );

        // Empêcher d'utiliser le cycle d'une autre école.
        if (!cycle.getEcole().getId().equals(ecole.getId())) {
            throw new IllegalArgumentException(
                    "Ce cycle n'appartient pas à cette école"
            );
        }

        List<ConfigurationCreneauxDto.CreneauDto> tries =
                dto.creneaux().stream()
                        .sorted(
                                Comparator.comparing(
                                        ConfigurationCreneauxDto.CreneauDto::jour
                                ).thenComparingInt(
                                        ConfigurationCreneauxDto.CreneauDto::heureDebut
                                )
                        )
                        .toList();

        String dernierJour = null;
        int derniereFin = -1;

        for (var c : tries) {
            if (!JOURS.contains(c.jour())) {
                throw new IllegalArgumentException(
                        "Jour invalide : " + c.jour()
                );
            }

            if (c.heureDebut() < 0 ||
                    c.heureFin() > 1440 ||
                    c.heureFin() <= c.heureDebut()) {
                throw new IllegalArgumentException(
                        "Horaires invalides pour " + c.jour()
                );
            }

            if (c.jour().equals(dernierJour) &&
                    c.heureDebut() < derniereFin) {
                throw new IllegalArgumentException(
                        "Deux créneaux se chevauchent le " + c.jour()
                );
            }

            dernierJour = c.jour();
            derniereFin = c.heureFin();
        }

        ConfigurationCreneaux config = configRepo
                .findByEcoleIdAndCycleId(
                        dto.ecoleId(), dto.cycleId()
                )
                .orElseGet(() -> {
                    ConfigurationCreneaux nouveau =
                            new ConfigurationCreneaux();
                    nouveau.setEcole(ecole);
                    nouveau.setCycle(cycle);
                    return nouveau;
                });

        config.getCreneaux().clear();

        for (var c : tries) {
            CreneauHoraire horaire = new CreneauHoraire();
            horaire.setConfiguration(config);
            horaire.setJour(c.jour());
            horaire.setHeureDebut(c.heureDebut());
            horaire.setHeureFin(c.heureFin());
            horaire.setOrdre(c.ordre());

            config.getCreneaux().add(horaire);
        }

        return versDto(configRepo.saveAndFlush(config));
    }

    private ConfigurationCreneauxDto versDto(
            ConfigurationCreneaux config
    ) {
        return new ConfigurationCreneauxDto(
                config.getEcole().getId(),
                config.getCycle().getId(),
                config.getCreneaux().stream()
                        .map(c ->
                                new ConfigurationCreneauxDto.CreneauDto(
                                        c.getJour(),
                                        c.getHeureDebut(),
                                        c.getHeureFin(),
                                        c.getOrdre()
                                )
                        )
                        .toList()
        );
    }
}