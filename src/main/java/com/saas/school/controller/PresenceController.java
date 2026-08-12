package com.saas.school.controller;

import com.saas.school.dto.PresenceResponseDTO;
import com.saas.school.entity.Presence;
import com.saas.school.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/presences")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PresenceController {

    private final PresenceService presenceService;

    @PutMapping("/toggle")
    public ResponseEntity<?> toggle(
            @RequestParam Long inscriptionId,
            @RequestParam Long edtId,
            @RequestParam(required = false) String date
    ) {
        try {
            LocalDate d = date != null
                    ? LocalDate.parse(date)
                    : LocalDate.now();

            PresenceResponseDTO dto =
                    presenceService.togglePresence(inscriptionId, edtId, d);

            return ResponseEntity.ok(dto);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/cours/{edtId}")
    public List<PresenceResponseDTO> getPresencesParCours(
            @PathVariable Long edtId,
            @RequestParam String date
    ) {
        return presenceService.getPresencesParCoursDto(edtId, LocalDate.parse(date));
    }

    @GetMapping("/classe/{classeId}/stats")
    public List<Map<String, Object>> getStatsParClasse(
            @PathVariable Long classeId,
            @RequestParam String date
    ) {
        return presenceService.getStatsParClasse(classeId, LocalDate.parse(date));
    }

    @GetMapping("/classe/{classeId}/eleves-inscriptions")
    public List<Map<String, Object>> getElevesAvecInscription(@PathVariable Long classeId) {
        return presenceService.getElevesAvecInscription(classeId);
    }
    @PutMapping("/classe/{classeId}/tout-present")
    public void marquerTousPresent(
            @PathVariable Long classeId,
            @RequestParam String jour,
            @RequestParam String date
    ) {
        presenceService.markAllPresent(classeId, jour, LocalDate.parse(date));
    }

    @PutMapping("/classe/{classeId}/tout-absent")
    public void marquerTousAbsent(
            @PathVariable Long classeId,
            @RequestParam String jour,
            @RequestParam String date
    ) {
        presenceService.markAllAbsent(classeId, jour, LocalDate.parse(date));
    }

    @GetMapping("/inscription/{inscriptionId}/absences")
    public long compterAbsences(
            @PathVariable Long inscriptionId,
            @RequestParam Long periodeId
    ) {
        return presenceService.compterAbsences(inscriptionId, periodeId);
    }
}