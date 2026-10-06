package com.saas.school.service;

import com.saas.school.dto.PresenceResponseDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // lectures par défaut ; les méthodes d'écriture surchargent avec @Transactional
public class PresenceService {

    private final PresenceRepository presenceRepository;
    private final InscriptionRepository inscriptionRepository;
    private final EmploiDuTempsRepository emploiDuTempsRepository;
    private final PeriodeRepository periodeRepository;
    private final BulletinMensuelInfoRepository bulletinMensuelInfoRepository;

    // Résout la période (trimestre) depuis la date, pour une année scolaire donnée
    private Periode resoudrePeriode(Long anneeScolaireId, LocalDate date) {
        return periodeRepository.findByDate(anneeScolaireId, date).orElse(null);
    }

    private Inscription trouverInscription(Long id) {
        return inscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Inscription introuvable"));
    }

    private EmploiDuTemps trouverEdt(Long id) {
        return emploiDuTempsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Emploi du temps introuvable"));
    }

    /* =========================================================
       BASCULE INDIVIDUELLE
    ========================================================= */

    /**
     * @deprecated Utiliser {@link #togglePresence(Long, Long, LocalDate)}, qui reçoit la date
     * en paramètre et partage la même logique de bascule.
     */
    @Deprecated
    @Transactional
    public void toggleAbsence(Long inscriptionId, Long edtId) {

        LocalDate today = LocalDate.now();

        Optional<Presence> existing =
                presenceRepository.findByInscriptionIdAndEmploiDuTempsIdAndDate(inscriptionId, edtId, today);

        if (existing.isPresent()) {
            presenceRepository.delete(existing.get());
        } else {
            Inscription inscription = trouverInscription(inscriptionId);
            EmploiDuTemps edt = trouverEdt(edtId);

            Presence p = new Presence();
            p.setInscription(inscription);
            p.setEmploiDuTemps(edt);
            p.setDate(today);
            p.setPeriode(resoudrePeriode(inscription.getAnneeScolaire().getId(), today));
            p.setStatut(Presence.StatutPresence.ABSENT);

            presenceRepository.save(p);
        }
    }

    /**
     * Bascule PRESENT ⇄ ABSENT.
     * Convention : l'absence d'enregistrement vaut « présent » (c'est ce qu'affiche l'écran).
     * Le premier clic crée donc un enregistrement ABSENT.
     */
    @Transactional
    public PresenceResponseDTO togglePresence(Long inscriptionId, Long edtId, LocalDate date) {

        Presence presence = presenceRepository
                .findByInscriptionIdAndEmploiDuTempsIdAndDate(inscriptionId, edtId, date)
                .orElseGet(() -> {
                    Inscription inscription = trouverInscription(inscriptionId);
                    EmploiDuTemps edt = trouverEdt(edtId);

                    Presence p = new Presence();
                    p.setInscription(inscription);
                    p.setEmploiDuTemps(edt);
                    p.setDate(date);
                    p.setPeriode(resoudrePeriode(inscription.getAnneeScolaire().getId(), date));
                    p.setStatut(Presence.StatutPresence.PRESENT); // ✅ état de départ = présent (corrige le 1er clic sans effet)
                    return p;
                });

        presence.setStatut(
                presence.getStatut() == Presence.StatutPresence.PRESENT
                        ? Presence.StatutPresence.ABSENT
                        : Presence.StatutPresence.PRESENT
        );

        return mapToDto(presenceRepository.save(presence));
    }

    /* =========================================================
       ACTIONS GROUPÉES — PAR COURS (et sous-groupe optionnel)
    ========================================================= */

    /**
     * Élèves visés : toute la classe (année active), ou uniquement ceux dont l'inscription
     * figure dans {@code inscriptionIds} (cas d'un sous-groupe). On recoupe avec la classe
     * pour ne jamais toucher une inscription étrangère à celle-ci.
     */
    private List<Inscription> inscriptionsCibles(Long classeId, List<Long> inscriptionIds) {
        List<Inscription> toutes = inscriptionRepository.findByClasseIdAndAnneeScolaire_ActiveTrue(classeId);

        if (inscriptionIds == null || inscriptionIds.isEmpty()) {
            return toutes;
        }

        Set<Long> ids = new HashSet<>(inscriptionIds);
        return toutes.stream().filter(i -> ids.contains(i.getId())).toList();
    }

    /**
     * Marque présents les élèves visés pour UN cours et UNE date.
     * Supprime leurs enregistrements (convention : pas d'enregistrement = présent).
     */
    @Transactional
    public void marquerCoursPresent(Long classeId, Long edtId, LocalDate date, List<Long> inscriptionIds) {

        trouverEdt(edtId); // vérifie l'existence du cours

        Set<Long> cibles = inscriptionsCibles(classeId, inscriptionIds).stream()
                .map(Inscription::getId)
                .collect(Collectors.toSet());

        List<Presence> aSupprimer = presenceRepository.findByEmploiDuTempsIdAndDate(edtId, date).stream()
                .filter(p -> cibles.contains(p.getInscription().getId()))
                .toList();

        presenceRepository.deleteAll(aSupprimer);
    }

    /**
     * Marque absents les élèves visés pour UN cours et UNE date.
     * Une seule requête de lecture + un seul saveAll (au lieu de N×M find/save).
     */
    @Transactional
    public void marquerCoursAbsent(Long classeId, Long edtId, LocalDate date, List<Long> inscriptionIds) {

        EmploiDuTemps edt = trouverEdt(edtId);
        List<Inscription> cibles = inscriptionsCibles(classeId, inscriptionIds);

        Map<Long, Presence> existantes = presenceRepository.findByEmploiDuTempsIdAndDate(edtId, date).stream()
                .collect(Collectors.toMap(p -> p.getInscription().getId(), p -> p, (a, b) -> a));

        Map<Long, Periode> periodesParAnnee = new HashMap<>();
        List<Presence> aSauvegarder = new ArrayList<>();

        for (Inscription inscription : cibles) {
            Presence p = existantes.get(inscription.getId());

            if (p == null) {
                p = new Presence();
                p.setInscription(inscription);
                p.setEmploiDuTemps(edt);
                p.setDate(date);

                Long anneeId = inscription.getAnneeScolaire().getId();
                if (!periodesParAnnee.containsKey(anneeId)) {
                    periodesParAnnee.put(anneeId, resoudrePeriode(anneeId, date)); // une seule requête par année
                }
                p.setPeriode(periodesParAnnee.get(anneeId));
            }

            p.setStatut(Presence.StatutPresence.ABSENT);
            aSauvegarder.add(p);
        }

        presenceRepository.saveAll(aSauvegarder);
    }

    /**
     * @deprecated S'applique à TOUS les cours du jour et ignore les sous-groupes.
     * Utiliser {@link #marquerCoursPresent(Long, Long, LocalDate, List)}.
     */
    @Deprecated
    @Transactional
    public void markAllPresent(Long classeId, String jour, LocalDate date) {
        List<EmploiDuTemps> cours = emploiDuTempsRepository.findByClasseIdAndJour(classeId, jour);
        for (EmploiDuTemps c : cours) {
            marquerCoursPresent(classeId, c.getId(), date, null);
        }
    }

    /**
     * @deprecated S'applique à TOUS les cours du jour et ignore les sous-groupes.
     * Utiliser {@link #marquerCoursAbsent(Long, Long, LocalDate, List)}.
     */
    @Deprecated
    @Transactional
    public void markAllAbsent(Long classeId, String jour, LocalDate date) {
        List<EmploiDuTemps> cours = emploiDuTempsRepository.findByClasseIdAndJour(classeId, jour);
        for (EmploiDuTemps c : cours) {
            marquerCoursAbsent(classeId, c.getId(), date, null);
        }
    }

    /* =========================================================
       LECTURES
    ========================================================= */

    public List<Presence> getPresencesParCours(Long edtId, LocalDate date) {
        return presenceRepository.findByEmploiDuTempsIdAndDate(edtId, date);
    }

    public List<PresenceResponseDTO> getPresencesParCoursDto(Long edtId, LocalDate date) {
        return getPresencesParCours(edtId, date).stream().map(this::mapToDto).toList();
    }

    /** Stats de présence d'une classe pour UNE journée précise. */
    public List<Map<String, Object>> getStatsParClasse(Long classeId, LocalDate date) {
        List<Presence> presences = presenceRepository.findByInscription_Classe_IdAndDate(classeId, date);
        return construireStats(presences);
    }

    /** Stats de présence d'une classe sur une PÉRIODE (debut → fin inclus), agrégées par élève. */
    public List<Map<String, Object>> getStatsParClassePeriode(Long classeId, LocalDate debut, LocalDate fin) {
        List<Presence> presences =
                presenceRepository.findByInscription_Classe_IdAndDateBetween(classeId, debut, fin);
        return construireStats(presences);
    }

    /** Agrégation par élève (présences/absences + taux), pour une journée ou une période. */
    private List<Map<String, Object>> construireStats(List<Presence> presences) {

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

    /** Historique détaillé (jour par jour) des présences d'UN élève sur une période. */
    public List<PresenceResponseDTO> getHistoriqueEleve(Long inscriptionId, LocalDate debut, LocalDate fin) {
        return presenceRepository
                .findByInscriptionIdAndDateBetweenOrderByDateAsc(inscriptionId, debut, fin)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    /** Nombre d'absences d'un élève sur une période (trimestre). */
    public long compterAbsences(Long inscriptionId, Long periodeId) {
        return presenceRepository.countByInscriptionIdAndPeriodeIdAndStatut(
                inscriptionId, periodeId, Presence.StatutPresence.ABSENT
        );
    }

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
    /** Absences du premier cycle : somme des compteurs mensuels du bulletin. */
    public Map<String, Object> getAbsencesPremierCycle(Long inscriptionId) {

        List<BulletinMensuelInfo> infos =
                bulletinMensuelInfoRepository.findByInscriptionIdOrderByIdAsc(inscriptionId);

        int total = infos.stream()
                .mapToInt(b -> b.getAbsences() == null ? 0 : b.getAbsences())
                .sum();

        List<Map<String, Object>> parMois = infos.stream()
                .map(b -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("mois", b.getMois());
                    m.put("absences", b.getAbsences() == null ? 0 : b.getAbsences());
                    return m;
                })
                .toList();

        Map<String, Object> resultat = new HashMap<>();
        resultat.put("total", total);
        resultat.put("parMois", parMois);
        return resultat;
    }
    // PresenceService
    public Map<String, Long> getResumeAbsences(Long inscriptionId) {
        long cours = presenceRepository.countByInscriptionIdAndStatut(
                inscriptionId, Presence.StatutPresence.ABSENT);
        long jours = presenceRepository.countJoursByInscriptionAndStatut(
                inscriptionId, Presence.StatutPresence.ABSENT);
        return Map.of("cours", cours, "jours", jours);
    }

}