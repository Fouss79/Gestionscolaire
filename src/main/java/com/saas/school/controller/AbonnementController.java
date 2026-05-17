package com.saas.school.controller;

import com.saas.school.dto.EcoleResponseDTO;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.PlanAbonnement;
import com.saas.school.entity.Utilisateur;
import com.saas.school.repository.UtilisateurRepository;
import com.saas.school.service.AbonnementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/superadmin/abonnements")
@RequiredArgsConstructor
public class AbonnementController {
private final UtilisateurRepository utilisateurRepository;
    private final AbonnementService service;

    @PutMapping("/{ecoleId}")
    public ResponseEntity<Ecole> assigner(
            @PathVariable Long ecoleId,
            @RequestParam String plan,
            @RequestParam int duree
    ) {

        PlanAbonnement p = PlanAbonnement.valueOf(plan.toUpperCase());

        Ecole updated = service.assignerPlan(ecoleId, p, duree);

        return ResponseEntity.ok(updated);
    }
    @GetMapping
    public List<EcoleResponseDTO> getAll() {
        return service.getAll();
    }
    @GetMapping("/me")
    public Map<String, Object> getMonAbonnement(@RequestHeader("X-USER-EMAIL") String email) {

        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User introuvable"));

        Ecole ecole = user.getEcole();

        return Map.of(
                "plan", ecole.getPlan(),
                "dateFin", ecole.getDateFin(),
                "active", ecole.isActive()
        );
    }


}