package com.saas.school.service;

import com.saas.school.dto.PresenceResponseDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final PresenceRepository presenceRepository;
    private final InscriptionRepository inscriptionRepository; // ⚠️ remplace EleveRepository
    private final EmploiDuTempsRepository emploiDuTempsRepository;
    private final PeriodeRepository periodeRepository; // ⚠️ nouveau

    // 🔥 Résout la période (trimestre) depuis la date, pour une année scolaire donnée
    private Periode resoudrePeriode(Long anneeScolaireId, LocalDate date) {
        return periodeRepository.findByDate(anneeScolaireId, date).orElse(null);
    }

    public void toggleAbsence(Long inscriptionId, Long edtId) {

        LocalDate today = LocalDate.now();

        Optional<Presence> existing =
                presenceRepository.findByInscriptionIdAndEmploiDuTempsIdAndDate(inscriptionId, edtId, today);

        if (existing.isPresent()) {
            presenceRepository.delete(existing.get());
        } else {

            Inscription inscription = inscriptionRepository.findById(inscriptionId)
                    .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

            EmploiDuTemps edt = emploiDuTempsRepository.findById(edtId)
                    .orElseThrow(() -> new RuntimeException("EDT introuvable"));

            Presence p = new Presence();
            p.setInscription(inscription);
            p.setEmploiDuTemps(edt);
            p.setDate(today);
            p.setPeriode(resoudrePeriode(inscription.getAnneeScolaire().getId(), today));
            p.setStatut(Presence.StatutPresence.ABSENT);

            presenceRepository.save(p);
        }
    }
    public PresenceResponseDTO togglePresence(Long inscriptionId, Long edtId, LocalDate date) {

        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        EmploiDuTemps edt = emploiDuTempsRepository.findById(edtId)
                .orElseThrow(() -> new RuntimeException("Emploi du temps introuvable"));

        Presence presence = presenceRepository
                .findByInscriptionIdAndEmploiDuTempsIdAndDate(inscriptionId, edtId, date)
                .orElseGet(() -> {
                    Presence p = new Presence();
                    p.setInscription(inscription);
                    p.setEmploiDuTemps(edt);
                    p.setDate(date);
                    p.setPeriode(resoudrePeriode(inscription.getAnneeScolaire().getId(), date));
                    return p;
                });

        presence.setStatut(
                presence.getStatut() == Presence.StatutPresence.PRESENT
                        ? Presence.StatutPresence.ABSENT
                        : Presence.StatutPresence.PRESENT
        );
       System.out.println(presence);
        Presence saved = presenceRepository.save(presence);
        return mapToDto(saved);
    }

    public List<Presence> getPresencesParCours(Long edtId, LocalDate date) {
        return presenceRepository.findByEmploiDuTempsIdAndDate(edtId, date);
    }

    public List<Map<String, Object>> getStatsParClasse(Long classeId, LocalDate date) {

        List<Presence> presences = presenceRepository.findByInscription_Classe_IdAndDate(classeId, date);

        Map<Long, Map<String, Object>> stats = new HashMap<>();

        for (Presence p : presences) {

            Long inscriptionId = p.getInscription().getId();
            Eleve eleve = p.getInscription().getEleve();

            stats.putIfAbsent(inscriptionId, new HashMap<>());
            Map<String, Object> s = stats.get(inscriptionId);

            s.put("inscriptionId", inscriptionId);
            s.put("nom", eleve.getNom() + " " + eleve.getPrenom());

            int present = ((Number) s.getOrDefault("present", 0)).intValue();
            int absent = ((Number) s.getOrDefault("absent", 0)).intValue();

            if (p.getStatut() == Presence.StatutPresence.PRESENT) present++;
            else absent++;

            s.put("present", present);
            s.put("absent", absent);
        }

        for (Map<String, Object> s : stats.values()) {
            int present = ((Number) s.get("present")).intValue();
            int absent = ((Number) s.get("absent")).intValue();
            int total = present + absent;
            double taux = total == 0 ? 100 : (present * 100.0 / total);
            s.put("taux", Math.round(taux));
        }

        return new ArrayList<>(stats.values());
    }

    /**
     * Rapport agrégé par période (trimestre) — utilise directement la Periode
     * déjà stockée sur chaque Presence, plutôt que de recalculer les dates.
     */
    public long compterAbsences(Long inscriptionId, Long periodeId) {
        return presenceRepository.countByInscriptionIdAndPeriodeIdAndStatut(
                inscriptionId, periodeId, Presence.StatutPresence.ABSENT
        );
    }
    public List<PresenceResponseDTO> getPresencesParCoursDto(Long edtId, LocalDate date) {
        return getPresencesParCours(edtId, date).stream().map(this::mapToDto).toList();
    }

    private PresenceResponseDTO mapToDto(Presence p) {

        PresenceResponseDTO dto = new PresenceResponseDTO();
        dto.setId(p.getId());
        dto.setInscriptionId(p.getInscription().getId());
        dto.setEleveNom(p.getInscription().getEleve().getNom());
        dto.setElevePrenom(p.getInscription().getEleve().getPrenom());

        if (p.getEmploiDuTemps() != null) {
            dto.setEdtId(p.getEmploiDuTemps().getId());
            dto.setMatiereNom(p.getEmploiDuTemps().getMatiere().getNom());
        }

        dto.setDate(p.getDate());
        dto.setStatut(p.getStatut().name());
        dto.setMotif(p.getMotif());

        return dto;
    }


    public void markAllPresent(Long classeId, String jour, LocalDate date) {
        List<EmploiDuTemps> cours = emploiDuTempsRepository.findByClasseIdAndJour(classeId, jour);
        for (EmploiDuTemps c : cours) {
            List<Presence> presences = presenceRepository.findByEmploiDuTempsIdAndDate(c.getId(), date);
            presenceRepository.deleteAll(presences);
        }
    }

    public void markAllAbsent(Long classeId, String jour, LocalDate date) {

        List<EmploiDuTemps> cours = emploiDuTempsRepository.findByClasseIdAndJour(classeId, jour);
        List<Inscription> inscriptions = inscriptionRepository.findByClasseIdAndAnneeScolaire_ActiveTrue(classeId);

        for (EmploiDuTemps c : cours) {
            for (Inscription inscription : inscriptions) {

                Presence p = presenceRepository
                        .findByInscriptionIdAndEmploiDuTempsIdAndDate(inscription.getId(), c.getId(), date)
                        .orElseGet(() -> {
                            Presence np = new Presence();
                            np.setInscription(inscription);
                            np.setEmploiDuTemps(c);
                            np.setDate(date);
                            np.setPeriode(resoudrePeriode(inscription.getAnneeScolaire().getId(), date));
                            return np;
                        });

                p.setStatut(Presence.StatutPresence.ABSENT);
                presenceRepository.save(p);
            }
        }
    }

    // Dans PresenceService ou un service dédié
    public List<Map<String, Object>> getElevesAvecInscription(Long classeId) {
        return inscriptionRepository.findByClasseIdAndAnneeScolaire_ActiveTrue(classeId)
                .stream()
                .map(i -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("inscriptionId", i.getId());
                    m.put("eleveId", i.getEleve().getId());
                    m.put("nom", i.getEleve().getNom());
                    m.put("prenom", i.getEleve().getPrenom());
                    return m;
                })
                .toList();
    }
}