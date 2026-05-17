package com.saas.school.service;
import  com.saas.school.entity.Paiement;
import com.saas.school.entity.PlanAbonnement;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.EleveRepository;
import com.saas.school.repository.EnseignantRepository;
import com.saas.school.repository.PaiementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SaasStatsService {

    private final EcoleRepository ecoleRepository;
    private final EleveRepository eleveRepository;
    private final EnseignantRepository enseignantRepository;
    private final PaiementRepository paiementRepository;

    // 📊 STATS GLOBALS
    public Map<String, Object> getGlobalStats() {

        long totalEcoles = ecoleRepository.count();
        long totalEleves = eleveRepository.count();
        long totalEnseignants = enseignantRepository.count();

        long ecolesActives = ecoleRepository.countByActiveTrue();

        long ecolesInactives = totalEcoles - ecolesActives;

        return Map.of(
                "totalEcoles", totalEcoles,
                "totalEleves", totalEleves,
                "totalEnseignants", totalEnseignants,
                "ecolesActives", ecolesActives,
                "ecolesInactives", ecolesInactives
        );
    }
    public Map<String, Object> getFullStats() {

        long totalEcoles = ecoleRepository.count();
        long totalEleves = eleveRepository.count();
        long totalEnseignants = enseignantRepository.count();

        long ecolesActives = ecoleRepository.countByActiveTrue();
        long ecolesInactives = totalEcoles - ecolesActives;

        long basic = ecoleRepository.countByPlan(PlanAbonnement.BASIC);
        long pro = ecoleRepository.countByPlan(PlanAbonnement.PRO);
        long premium = ecoleRepository.countByPlan(PlanAbonnement.PREMIUM);

        return Map.of(
                "totalEcoles", totalEcoles,
                "totalEleves", totalEleves,
                "totalEnseignants", totalEnseignants,
                "ecolesActives", ecolesActives,
                "ecolesInactives", ecolesInactives,
                "basic", basic,
                "pro", pro,
                "premium", premium
        );
    }

    public double totalRevenue() {
        return paiementRepository.findAll().stream()
                .filter(p -> p.getStatus().equals("SUCCESS"))
                .mapToDouble(Paiement::getMontant)
                .sum();
    }

    // 🔥 STATS PAR PLAN
    public Map<String, Long> getPlanStats() {

        long basic = ecoleRepository.countByPlan(PlanAbonnement.BASIC);
        long pro = ecoleRepository.countByPlan(PlanAbonnement.PRO);
        long premium = ecoleRepository.countByPlan(PlanAbonnement.PREMIUM);

        return Map.of(
                "BASIC", basic,
                "PRO", pro,
                "PREMIUM", premium
        );
    }
}