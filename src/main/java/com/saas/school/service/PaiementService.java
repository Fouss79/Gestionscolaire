package com.saas.school.service;

import com.saas.school.entity.Paiement;
import com.saas.school.entity.PlanAbonnement;
import com.saas.school.repository.PaiementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaiementService {

    private final PaiementRepository paiementRepository;

    public Paiement creerPaiement(Long ecoleId, PlanAbonnement plan, int duree) {

        double montant = calculerMontant(plan, duree);

        Paiement p = new Paiement();
        p.setEcoleId(ecoleId);
        p.setPlan(plan);
        p.setDuree(duree);
        p.setMontant(montant);
        p.setStatus("PENDING");
        p.setCreatedAt(LocalDateTime.now());

        return paiementRepository.save(p);
    }

    private double calculerMontant(PlanAbonnement plan, int duree) {

        return switch (plan) {
            case BASIC -> 5000 * duree;
            case PRO -> 10000 * duree;
            case PREMIUM -> 20000 * duree;
        };
    }

    public void validerPaiement(Long paiementId) {

        Paiement p = paiementRepository.findById(paiementId)
                .orElseThrow();

        p.setStatus("SUCCESS");

        paiementRepository.save(p);
    }
}