package com.saas.school.service;

import com.saas.school.dto.EmargementDTO;
import com.saas.school.dto.EmargementResumeDTO;
import com.saas.school.entity.Emargement;
import com.saas.school.entity.EmploiDuTemps;
import com.saas.school.entity.Enseignant;
import com.saas.school.repository.EmargementRepository;
import com.saas.school.repository.EmploiDuTempsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmargementService {

    private final EmargementRepository emargementRepo;
    private final EmploiDuTempsRepository emploiRepo;

    // ================= EMPLOI PAR DATE =================
    public List<EmploiDuTemps> getEmploiParDate(LocalDate date, Long anneeId) {

        DayOfWeek day = date.getDayOfWeek();

        String jour = switch (day) {
            case MONDAY -> "LUNDI";
            case TUESDAY -> "MARDI";
            case WEDNESDAY -> "MERCREDI";
            case THURSDAY -> "JEUDI";
            case FRIDAY -> "VENDREDI";
            case SATURDAY -> "SAMEDI";
            case SUNDAY -> "DIMANCHE";
        };

        return emploiRepo.findByJourAndAnneeScolaireId(jour, anneeId);
    }

    // ================= EMARGEMENTS PAR DATE =================
    public List<Emargement> getEmargementsParDate(LocalDate date) {
        return emargementRepo.findByDateHeure(date);
    }

    // ================= EMARGEMENTS PAR ENSEIGNANT (filtré par école via anneeId) =================
    public List<EmargementDTO> getEmargementsParEnseignant(Long enseignantId, Long anneeId) {
        return emargementRepo
                .findByEmploiDuTemps_Enseignant_IdAndDateHeureBetweenAndEmploiDuTemps_AnneeScolaireId(
                        enseignantId, LocalDate.of(2000, 1, 1), LocalDate.now().plusYears(1), anneeId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<EmargementDTO> getEmargementsParEnseignantEtPeriode(
            Long enseignantId, LocalDate debut, LocalDate fin, Long anneeId) {
        return emargementRepo
                .findByEmploiDuTemps_Enseignant_IdAndDateHeureBetweenAndEmploiDuTemps_AnneeScolaireId(
                        enseignantId, debut, fin, anneeId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<EmargementDTO> getEmargementsParDateDTO(LocalDate date) {
        return emargementRepo.findByDateHeure(date)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // ================= RESUME PAR ENSEIGNANT (filtré par école via anneeId) =================
    public EmargementResumeDTO getResumeParEnseignant(
            Long enseignantId, LocalDate debut, LocalDate fin, Long anneeId) {

        List<Emargement> emargements = emargementRepo
                .findByEmploiDuTemps_Enseignant_IdAndDateHeureBetweenAndEmploiDuTemps_AnneeScolaireId(
                        enseignantId, debut, fin, anneeId);

        int totalSeances = emargements.size();
        int totalHeures = emargements.stream().mapToInt(Emargement::getDuree).sum();

        // Créneaux prévus pour cet enseignant, dans cette école/année uniquement
        List<EmploiDuTemps> creneaux = emploiRepo
                .findByEnseignant_IdAndAnneeScolaireId(enseignantId, anneeId);

        int totalSeancesPrevues = 0;
        for (EmploiDuTemps edt : creneaux) {
            totalSeancesPrevues += compterOccurrencesJour(edt.getJour(), debut, fin);
        }

        double taux = totalSeancesPrevues == 0
                ? 0.0
                : (totalSeances * 100.0) / totalSeancesPrevues;

        String nom = null, prenom = null;
        if (!creneaux.isEmpty() && creneaux.get(0).getEnseignant() != null) {
            nom = creneaux.get(0).getEnseignant().getNom();
            prenom = creneaux.get(0).getEnseignant().getPrenom();
        }

        return EmargementResumeDTO.builder()
                .enseignantId(enseignantId)
                .enseignantNom(nom)
                .enseignantPrenom(prenom)
                .periodeDebut(debut)
                .periodeFin(fin)
                .totalSeances(totalSeances)
                .totalSeancesPrevues(totalSeancesPrevues)
                .totalHeuresEmargees(totalHeures)
                .tauxPresence(Math.round(taux * 100.0) / 100.0)
                .build();
    }

    // ================= RESUME TOUS LES ENSEIGNANTS (filtré par école via anneeId) =================
    public List<EmargementResumeDTO> getResumeTousEnseignants(LocalDate debut, LocalDate fin, Long anneeId) {

        // 1. Créneaux d'EDT UNIQUEMENT pour cette année/école
        List<EmploiDuTemps> tousCreneaux = emploiRepo.findByAnneeScolaireId(anneeId);

        // 2. Émargements UNIQUEMENT pour cette année/école, sur la période
        List<Emargement> tousEmargements = emargementRepo
                .findByDateHeureBetweenAndEmploiDuTemps_AnneeScolaireId(debut, fin, anneeId);

        // 3. Regrouper les créneaux par enseignant
        Map<Long, List<EmploiDuTemps>> creneauxParEnseignant = tousCreneaux.stream()
                .filter(edt -> edt.getEnseignant() != null)
                .collect(Collectors.groupingBy(edt -> edt.getEnseignant().getId()));

        // 4. Regrouper les émargements par enseignant
        Map<Long, List<Emargement>> emargementsParEnseignant = tousEmargements.stream()
                .filter(em -> em.getEmploiDuTemps() != null && em.getEmploiDuTemps().getEnseignant() != null)
                .collect(Collectors.groupingBy(em -> em.getEmploiDuTemps().getEnseignant().getId()));

        List<EmargementResumeDTO> resultats = new ArrayList<>();

        for (Map.Entry<Long, List<EmploiDuTemps>> entry : creneauxParEnseignant.entrySet()) {

            Long enseignantId = entry.getKey();
            List<EmploiDuTemps> creneaux = entry.getValue();
            List<Emargement> emargements = emargementsParEnseignant.getOrDefault(enseignantId, List.of());

            int totalSeancesPrevues = 0;
            for (EmploiDuTemps edt : creneaux) {
                totalSeancesPrevues += compterOccurrencesJour(edt.getJour(), debut, fin);
            }

            int totalSeances = emargements.size();
            int totalHeures = emargements.stream().mapToInt(Emargement::getDuree).sum();

            double taux = totalSeancesPrevues == 0
                    ? 0.0
                    : (totalSeances * 100.0) / totalSeancesPrevues;

            Enseignant enseignant = creneaux.get(0).getEnseignant();

            resultats.add(EmargementResumeDTO.builder()
                    .enseignantId(enseignantId)
                    .enseignantNom(enseignant.getNom())
                    .enseignantPrenom(enseignant.getPrenom())
                    .periodeDebut(debut)
                    .periodeFin(fin)
                    .totalSeances(totalSeances)
                    .totalSeancesPrevues(totalSeancesPrevues)
                    .totalHeuresEmargees(totalHeures)
                    .tauxPresence(Math.round(taux * 100.0) / 100.0)
                    .build());
        }

        resultats.sort(Comparator.comparingDouble(EmargementResumeDTO::getTauxPresence));

        return resultats;
    }

    // ================= EMARGER =================
    public Emargement emarger(EmploiDuTemps edt, LocalDate date) {

        if (date == null) {
            throw new RuntimeException("Date obligatoire");
        }

        boolean exists = emargementRepo
                .existsByEmploiDuTemps_IdAndDateHeure(edt.getId(), date);

        if (exists) {
            throw new RuntimeException("Déjà émargé");
        }

        int duree = edt.getHeureFin() - edt.getHeureDebut();

        Emargement em = new Emargement();
        em.setEmploiDuTemps(edt);
        em.setDateHeure(date);
        em.setJour(edt.getJour());
        em.setPresent(true);
        em.setDuree(duree);

        return emargementRepo.save(em);
    }

    // ================= HELPERS =================
    private int compterOccurrencesJour(String jourStr, LocalDate debut, LocalDate fin) {
        DayOfWeek jourCible = switch (jourStr.toUpperCase()) {
            case "LUNDI" -> DayOfWeek.MONDAY;
            case "MARDI" -> DayOfWeek.TUESDAY;
            case "MERCREDI" -> DayOfWeek.WEDNESDAY;
            case "JEUDI" -> DayOfWeek.THURSDAY;
            case "VENDREDI" -> DayOfWeek.FRIDAY;
            case "SAMEDI" -> DayOfWeek.SATURDAY;
            case "DIMANCHE" -> DayOfWeek.SUNDAY;
            default -> throw new RuntimeException("Jour invalide: " + jourStr);
        };

        int count = 0;
        LocalDate date = debut;
        while (!date.isAfter(fin)) {
            if (date.getDayOfWeek() == jourCible) {
                count++;
            }
            date = date.plusDays(1);
        }
        return count;
    }

    private EmargementDTO toDTO(Emargement em) {
        EmploiDuTemps edt = em.getEmploiDuTemps();

        return EmargementDTO.builder()
                .id(em.getId())
                .dateHeure(em.getDateHeure())
                .jour(em.getJour())
                .present(em.isPresent())
                .duree(em.getDuree())
                .edtId(edt.getId())
                .matiere(edt.getMatiere() != null ? edt.getMatiere().getNom() : null)
                .classe(edt.getClasse() != null ? edt.getClasse().getNomComplet() : null)
                .heureDebut(edt.getHeureDebut())
                .heureFin(edt.getHeureFin())
                .enseignantId(edt.getEnseignant() != null ? edt.getEnseignant().getId() : null)
                .enseignantNom(edt.getEnseignant() != null ? edt.getEnseignant().getNom() : null)
                .enseignantPrenom(edt.getEnseignant() != null ? edt.getEnseignant().getPrenom() : null)
                .build();
    }
}