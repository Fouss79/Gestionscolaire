package com.saas.school.service;

import com.saas.school.entity.Emargement;
import com.saas.school.entity.EmploiDuTemps;
import com.saas.school.repository.EmargementRepository;
import com.saas.school.repository.EmploiDuTempsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

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
}