package com.saas.school.controller;

import com.saas.school.entity.Paiement;
import com.saas.school.entity.PlanAbonnement;
import com.saas.school.entity.Presence;
import com.saas.school.repository.PaiementRepository;
import com.saas.school.repository.PresenceRepository;
import com.saas.school.service.AbonnementService;
import com.saas.school.service.PaiementService;
import com.saas.school.service.PresenceService;
import org.springframework.web.servlet.view.RedirectView;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;
    private final PresenceRepository presenceRepository;

    @PostMapping("/toggle")
    public Presence togglePresence(@RequestParam Long eleveId,
                                   @RequestParam Long edtId) {
        return presenceService.togglePresence(eleveId, edtId);
    }

    @GetMapping("/cours/{edtId}/date")
    public List<Presence> getPresencesByDate(
            @PathVariable Long edtId,
            @RequestParam String date) {

        return presenceRepository.findByEmploiDuTempsIdAndDate(
                edtId, LocalDate.parse(date)
        );
    }

    @GetMapping("/cours/{edtId}")
    public List<Presence> getPresences(@PathVariable Long edtId) {
        return presenceService.getPresencesParCours(edtId);
    }
    @GetMapping("/stats/classe/{classeId}")
    public List<Map<String, Object>>stats(@PathVariable Long classeId) {
        return presenceService.getStatsParClasse(classeId);
    }
    // 🔥 NOUVEAU : tout présent
    @PostMapping("/all-present")
    public void markAllPresent(@RequestBody Map<String, Object> body) {
        Long classeId = Long.valueOf(body.get("classeId").toString());
        String jour = body.get("jour").toString();

        presenceService.markAllPresent(classeId, jour);
    } @PostMapping("/toggle-absence")
    public void toggleAbsence(@RequestParam Long eleveId,
                              @RequestParam Long edtId) {
        presenceService.toggleAbsence(eleveId, edtId);
    }

    // 🔥 NOUVEAU : tout absent
    @PostMapping("/all-absent")
    public void markAllAbsent(@RequestBody Map<String, Object> body) {
        Long classeId = Long.valueOf(body.get("classeId").toString());
        String jour = body.get("jour").toString();

        presenceService.markAllAbsent(classeId, jour);
    }
}