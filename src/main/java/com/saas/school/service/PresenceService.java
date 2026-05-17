package com.saas.school.service;

import com.saas.school.entity.Eleve;
import com.saas.school.entity.EmploiDuTemps;
import com.saas.school.entity.Presence;
import com.saas.school.repository.EleveRepository;
import com.saas.school.repository.EmploiDuTempsRepository;
import com.saas.school.repository.PresenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final PresenceRepository presenceRepository;
    private final EleveRepository eleveRepository;
    private final EmploiDuTempsRepository emploiDuTempsRepository;

    public void toggleAbsence(Long eleveId, Long edtId) {

        LocalDate today = LocalDate.now();

        Optional<Presence> existing =
                presenceRepository.findByEleveIdAndEmploiDuTempsIdAndDate(
                        eleveId, edtId, today
                );

        if (existing.isPresent()) {
            // 🔥 présent → suppression
            presenceRepository.delete(existing.get());
        } else {
            // 🔥 absent → création
            Presence p = new Presence();

            Eleve e = eleveRepository.findById(eleveId)
                    .orElseThrow(() -> new RuntimeException("Élève introuvable"));

            EmploiDuTemps edt = emploiDuTempsRepository.findById(edtId)
                    .orElseThrow(() -> new RuntimeException("EDT introuvable"));

            p.setEleve(e);
            p.setEmploiDuTemps(edt);
            p.setDate(today);
            p.setStatut(StatutPresence.ABSENT);

            presenceRepository.save(p);
        }
    }
    public Presence togglePresence(Long eleveId, Long edtId) {

        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new RuntimeException("Élève introuvable"));

        EmploiDuTemps edt = emploiDuTempsRepository.findById(edtId)
                .orElseThrow(() -> new RuntimeException("Emploi du temps introuvable"));

        LocalDate today = LocalDate.now();

        Presence presence = presenceRepository
                .findByEleveIdAndEmploiDuTempsIdAndDate(eleveId, edtId, today)
                .orElseGet(() -> {
                    Presence p = new Presence();
                    p.setEleve(eleve);
                    p.setEmploiDuTemps(edt);
                    p.setDate(today);

                    // 🔥 PRÉSENT PAR DÉFAUT
                    p.setStatut(StatutPresence.PRESENT);

                    return p;
                });

        // 🔥 TOGGLE PRO
        presence.setStatut(
                presence.getStatut() == StatutPresence.PRESENT
                        ? StatutPresence.ABSENT
                        : StatutPresence.PRESENT
        );

        return presenceRepository.save(presence);
    }

    public List<Presence> getPresencesParCours(Long edtId) {
        return presenceRepository.findByEmploiDuTempsIdAndDate(
                edtId,
                LocalDate.now()
        );
    }
    public List<Map<String, Object>> getStatsParClasse(Long classeId) {

        List<Presence> presences = presenceRepository.findByEleveClasseId(classeId);

        Map<Long, Map<String, Object>> stats = new HashMap<>();

        for (Presence p : presences) {

            Long eleveId = p.getEleve().getId();

            stats.putIfAbsent(eleveId, new HashMap<>());

            Map<String, Object> s = stats.get(eleveId);

            s.put("eleveId", eleveId);
            s.put("nom", p.getEleve().getNom() + " " + p.getEleve().getPrenom());

            int present = ((Number) s.getOrDefault("present", 0)).intValue();
            int absent = ((Number) s.getOrDefault("absent", 0)).intValue();

            if (p.getStatut() == StatutPresence.PRESENT) {
                present++;
            } else {
                absent++;
            }

            s.put("present", present);
            s.put("absent", absent);
        }

        // 🔥 calcul taux
        for (Map<String, Object> s : stats.values()) {
            int present = ((Number) s.getOrDefault("present", 0)).intValue();
            int absent = ((Number) s.getOrDefault("absent", 0)).intValue();

            int total = present + absent;

            double taux = total == 0 ? 100 : (present * 100.0 / total);

            s.put("taux", Math.round(taux));
        }

        return new ArrayList<>(stats.values());
    }
    public void markAllPresent(Long classeId, String jour) {

        LocalDate today = LocalDate.now();

        List<EmploiDuTemps> cours = emploiDuTempsRepository
                .findByClasseIdAndJour(classeId, jour);

        for (EmploiDuTemps c : cours) {
            List<Presence> presences = presenceRepository
                    .findByEmploiDuTempsIdAndDate(c.getId(), today);

            // 🔥 suppression = tous présents
            presenceRepository.deleteAll(presences);
        }
    }
    public void markAllAbsent(Long classeId, String jour) {

        LocalDate today = LocalDate.now();

        List<EmploiDuTemps> cours = emploiDuTempsRepository
                .findByClasseIdAndJour(classeId, jour);

        List<Eleve> eleves = eleveRepository.findByClasseId(classeId);

        for (EmploiDuTemps c : cours) {
            for (Eleve e : eleves) {

                Presence p = presenceRepository
                        .findByEleveIdAndEmploiDuTempsIdAndDate(e.getId(), c.getId(), today)
                        .orElseGet(() -> {
                            Presence np = new Presence();
                            np.setEleve(e);
                            np.setEmploiDuTemps(c);
                            np.setDate(today);
                            return np;
                        });

                p.setStatut(StatutPresence.ABSENT);

                presenceRepository.save(p);
            }
        }
    }

}