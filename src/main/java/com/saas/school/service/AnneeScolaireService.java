package com.saas.school.service;

import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Ecole;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.EcoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnneeScolaireService {

    private final AnneeScolaireRepository anneeRepository;
    private final EcoleRepository ecoleRepository;

    // 🔥 créer année scolaire
    public AnneeScolaire creer(
            String nom,
            LocalDate dateDebut,
            LocalDate dateFin,
            Long ecoleId
    ) {

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        if (anneeRepository.existsByNomAndEcoleId(nom, ecoleId)) {
            throw new RuntimeException("Cette année existe déjà pour cette école");
        }

        if (dateFin.isBefore(dateDebut)) {
            throw new RuntimeException(
                    "La date de fin doit être supérieure à la date de début"
            );
        }

        AnneeScolaire annee = new AnneeScolaire();
        annee.setNom(nom);
        annee.setDateDebut(dateDebut);
        annee.setDateFin(dateFin);
        annee.setEcole(ecole);
        annee.setActive(false);
        annee.setCreatedAt(LocalDateTime.now());

        return anneeRepository.save(annee);
    }
    public class AnneeScolaireUtils {

        public static List<YearMonth> getMois(AnneeScolaire annee) {

            List<YearMonth> mois = new ArrayList<>();

            YearMonth courant = YearMonth.from(annee.getDateDebut());
            YearMonth fin = YearMonth.from(annee.getDateFin());

            while (!courant.isAfter(fin)) {
                mois.add(courant);
                courant = courant.plusMonths(1);
            }

            return mois;
        }
    }
    // 📥 toutes les années d'une école
    public List<AnneeScolaire> getByEcole(Long ecoleId) {
        return anneeRepository.findByEcoleId(ecoleId);
    }

    // 🔥 activer une année (une seule active)
    public AnneeScolaire activer(Long id) {

        AnneeScolaire annee = anneeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Année introuvable"));

        // désactiver les autres
        List<AnneeScolaire> annees = anneeRepository.findByEcoleId(annee.getEcole().getId());
        for (AnneeScolaire a : annees) {
            a.setActive(false);
        }
        anneeRepository.saveAll(annees);

        // activer celle-ci
        annee.setActive(true);
        return anneeRepository.save(annee);
    }

    // 📥 année active
    public AnneeScolaire getActive(Long ecoleId) {
        return anneeRepository.findByEcoleIdAndActiveTrue(ecoleId)
                .orElseThrow(() -> new RuntimeException("Aucune année active"));
    }
}