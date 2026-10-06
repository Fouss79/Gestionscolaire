package com.saas.school.controller;

import com.saas.school.dto.PresenceResponseDTO;
import com.saas.school.service.PresenceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/presences")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PresenceController {

    private final PresenceService presenceService;

    /* =========================================================
       BASCULE INDIVIDUELLE
    ========================================================= */

    @PutMapping("/toggle")
    public PresenceResponseDTO toggle(
            @RequestParam Long inscriptionId,
            @RequestParam Long edtId,
            @RequestParam(required = false) String date
    ) {
        LocalDate d = date != null ? LocalDate.parse(date) : LocalDate.now();
        return presenceService.togglePresence(inscriptionId, edtId, d);
    }

    /* =========================================================
       ACTIONS GROUPÉES — PAR COURS (sous-groupe optionnel)
    ========================================================= */

    /**
     * PUT /api/presences/classe/{classeId}/cours/{edtId}/tout-present?date=2026-10-05
     * Sous-groupe : ajouter &inscriptionIds=1&inscriptionIds=2...
     * Sans inscriptionIds : toute la classe (année active).
     */
    @PutMapping("/classe/{classeId}/cours/{edtId}/tout-present")
    public ResponseEntity<Void> toutPresentCours(
            @PathVariable Long classeId,
            @PathVariable Long edtId,
            @RequestParam String date,
            @RequestParam(required = false) List<Long> inscriptionIds
    ) {
        presenceService.marquerCoursPresent(classeId, edtId, LocalDate.parse(date), inscriptionIds);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/classe/{classeId}/cours/{edtId}/tout-absent")
    public ResponseEntity<Void> toutAbsentCours(
            @PathVariable Long classeId,
            @PathVariable Long edtId,
            @RequestParam String date,
            @RequestParam(required = false) List<Long> inscriptionIds
    ) {
        presenceService.marquerCoursAbsent(classeId, edtId, LocalDate.parse(date), inscriptionIds);
        return ResponseEntity.noContent().build();
    }

    /**
     * @deprecated S'applique à tous les cours du jour et ignore les sous-groupes.
     * Remplacé par {@code /classe/{classeId}/cours/{edtId}/tout-present}.
     */
    @Deprecated
    @PutMapping("/classe/{classeId}/tout-present")
    public void marquerTousPresent(
            @PathVariable Long classeId,
            @RequestParam String jour,
            @RequestParam String date
    ) {
        presenceService.markAllPresent(classeId, jour, LocalDate.parse(date));
    }

    /**
     * @deprecated S'applique à tous les cours du jour et ignore les sous-groupes.
     * Remplacé par {@code /classe/{classeId}/cours/{edtId}/tout-absent}.
     */
    @Deprecated
    @PutMapping("/classe/{classeId}/tout-absent")
    public void marquerTousAbsent(
            @PathVariable Long classeId,
            @RequestParam String jour,
            @RequestParam String date
    ) {
        presenceService.markAllAbsent(classeId, jour, LocalDate.parse(date));
    }

    /* =========================================================
       LECTURES
    ========================================================= */

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

    /**
     * Stats de présence d'une classe sur une PÉRIODE — présences/absences cumulées et taux par élève.
     * GET /api/presences/classe/{classeId}/stats-periode?debut=2026-01-01&fin=2026-01-31
     */
    @GetMapping("/classe/{classeId}/stats-periode")
    public List<Map<String, Object>> getStatsParClassePeriode(
            @PathVariable Long classeId,
            @RequestParam String debut,
            @RequestParam String fin
    ) {
        return presenceService.getStatsParClassePeriode(
                classeId, LocalDate.parse(debut), LocalDate.parse(fin)
        );
    }

    @GetMapping("/classe/{classeId}/eleves-inscriptions")
    public List<Map<String, Object>> getElevesAvecInscription(@PathVariable Long classeId) {
        return presenceService.getElevesAvecInscription(classeId);
    }

    @GetMapping("/inscription/{inscriptionId}/absences")
    public long compterAbsences(
            @PathVariable Long inscriptionId,
            @RequestParam Long periodeId
    ) {
        return presenceService.compterAbsences(inscriptionId, periodeId);
    }

    /**
     * Historique détaillé (jour par jour) des présences d'un élève sur une période.
     * GET /api/presences/inscription/{inscriptionId}/historique?debut=2026-01-01&fin=2026-01-31
     */
    @GetMapping("/inscription/{inscriptionId}/historique")
    public List<PresenceResponseDTO> getHistoriqueEleve(
            @PathVariable Long inscriptionId,
            @RequestParam String debut,
            @RequestParam String fin
    ) {
        return presenceService.getHistoriqueEleve(
                inscriptionId, LocalDate.parse(debut), LocalDate.parse(fin)
        );
    }

    /* =========================================================
       GESTION D'ERREURS (locale à ce controller)
    ========================================================= */

    /** Inscription / cours introuvable → 404 avec un message lisible. */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    /** Date mal formatée (attendu : AAAA-MM-JJ) → 400. */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, String>> dateInvalide(DateTimeParseException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Date invalide, format attendu : AAAA-MM-JJ"));
    }
    @GetMapping("/inscription/{inscriptionId}/absences/premier-cycle")
    public Map<String, Object> getAbsencesPremierCycle(@PathVariable Long inscriptionId) {
        return presenceService.getAbsencesPremierCycle(inscriptionId);
    }
    // PresenceController
    @GetMapping("/inscription/{inscriptionId}/absences/resume")
    public Map<String, Long> getResumeAbsences(@PathVariable Long inscriptionId) {
        return presenceService.getResumeAbsences(inscriptionId);
    }

}