package com.saas.school.service;

import com.saas.school.dto.EcoleResponseDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AbonnementService {

    private final EcoleRepository ecoleRepository;
    private final AbonnementRepository abonnementRepository;

    public Ecole assignerPlan(Long ecoleId, PlanAbonnement plan, int duree) {

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        // 🔥 calcul dates
        LocalDate now = LocalDate.now();
        LocalDate fin = now.plusMonths(duree);

        // 🔥 créer abonnement
        Abonnement abo = new Abonnement();
        abo.setEcole(ecole);
        abo.setPlan(plan);
        abo.setDateDebut(now);
        abo.setDateFin(fin);
        abo.setActif(true);

        abonnementRepository.save(abo); // 🔥 ICI C’EST LA CLÉ

        // 🔥 (optionnel) mettre plan sur école
        ecole.setPlan(plan);
        ecole.setDateFin(fin);

        return ecoleRepository.save(ecole);
    }
    public List<EcoleResponseDTO> getAll() {

        return ecoleRepository.findAll().stream().map(e -> {

            Abonnement abo = abonnementRepository
                    .findTopByEcoleIdOrderByDateFinDesc(e.getId())
                    .orElse(null);

            EcoleResponseDTO dto = new EcoleResponseDTO();

            dto.setId(e.getId());
            dto.setNom(e.getNom());
            dto.setVille(e.getVille());
            dto.setPays(e.getPays());
            dto.setActive(e.isActive());

            if (abo != null) {
                dto.setPlan(abo.getPlan().name());
                dto.setDateFin(abo.getDateFin());
            }

            return dto;

        }).toList();
    }

    // 🔥 Vérifier si abonnement actif
    public boolean isActif(Ecole ecole) {

        if (ecole.getDateFin() == null) return false;

        return ecole.getDateFin().isAfter(LocalDate.now());
    }

    // 🔥 Bloquer si expiré
    public void verifierAbonnement(Ecole ecole) {
        if (!isActif(ecole)) {
            throw new RuntimeException("Abonnement expiré ❌");
        }
    }
    public boolean isExpire(Ecole ecole) {

        Abonnement abo = abonnementRepository
                .findTopByEcoleIdOrderByDateFinDesc(ecole.getId())
                .orElse(null);

        if (abo == null) return true;

        return abo.getDateFin().isBefore(LocalDate.now());
    }
    public boolean expireBientot(Ecole ecole) {

        Abonnement abo = abonnementRepository
                .findTopByEcoleIdOrderByDateFinDesc(ecole.getId())
                .orElse(null);

        if (abo == null) return false;

        return abo.getDateFin().isBefore(LocalDate.now().plusDays(7));
    }

}