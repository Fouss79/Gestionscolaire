package com.saas.school.service;

import com.saas.school.entity.Emargement;
import com.saas.school.entity.EmploiDuTemps;
import com.saas.school.repository.EmargementRepository;
import com.saas.school.repository.EmploiDuTempsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmargementService {

    private final EmargementRepository emargementRepo;
    private final EmploiDuTempsRepository emploiRepo;

    // ================= 🔹 EMPLOI DU TEMPS =================
    public List<EmploiDuTemps> getEmploiParJour(String jour, Long anneeId) {
        return emploiRepo.findByJourAndAnneeScolaireId(jour, anneeId);
    }

    // ================= 🔹 EMARGEMENTS PAR JOUR =================
    public List<Emargement> getEmargementsParJour(String jour, LocalDate date) {
        return emargementRepo.findByJourAndDateHeure(jour, date);
    }

    // ================= 🔹 EMARGER =================
    public Emargement emarger(EmploiDuTemps edt, LocalDate date) {

        // 🔥 Vérifier si déjà émargé
        boolean exists = emargementRepo.existsByEnseignant_IdAndClasse_IdAndMatiere_IdAndDateHeure(
                edt.getEnseignant().getId(),
                edt.getClasse().getId(),
                edt.getMatiere().getId(),
                date
        );

        if (exists) {
            throw new RuntimeException("Déjà émargé");
        }

        // 🔥 Calcul durée
        int duree = edt.getHeureFin() - edt.getHeureDebut();

        // 🔥 Création
        Emargement emargement = new Emargement();
        emargement.setJour(edt.getJour());
        emargement.setDateHeure(date);
        emargement.setPresent(true);

        // relations
        emargement.setEnseignant(edt.getEnseignant());
        emargement.setClasse(edt.getClasse());
        emargement.setMatiere(edt.getMatiere());
        emargement.setAnneeScolaire(edt.getAnneeScolaire());

        // obligatoire
        emargement.setDuree(duree);

        return emargementRepo.save(emargement);
    }
}