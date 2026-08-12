package com.saas.school.service;

import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Periode;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.PeriodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PeriodeService {

    private final PeriodeRepository periodeRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;
    private final EcoleRepository ecoleRepository;

    public Periode creer(String nom, Integer ordre, LocalDate dateDebut, LocalDate dateFin,
                         Long anneeScolaireId, Long ecoleId) {

        AnneeScolaire annee = anneeScolaireRepository.findById(anneeScolaireId)
                .orElseThrow(() -> new RuntimeException("Année scolaire introuvable"));

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        if (dateDebut != null && dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new RuntimeException("La date de fin doit être après la date de début");
        }

        Periode periode = new Periode();
        periode.setNom(nom);
        periode.setOrdre(ordre);
        periode.setDateDebut(dateDebut);
        periode.setDateFin(dateFin);
        periode.setAnneeScolaire(annee);
        periode.setEcole(ecole);

        return periodeRepository.save(periode);
    }

    public List<Periode> getByEcoleEtAnnee(Long ecoleId, Long anneeScolaireId) {
        return periodeRepository.findByEcoleIdAndAnneeScolaireIdOrderByOrdreAsc(ecoleId, anneeScolaireId);
    }

    public Periode modifier(Long id, String nom, Integer ordre, LocalDate dateDebut, LocalDate dateFin) {

        Periode periode = periodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Période introuvable"));

        if (dateDebut != null && dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new RuntimeException("La date de fin doit être après la date de début");
        }

        periode.setNom(nom);
        periode.setOrdre(ordre);
        periode.setDateDebut(dateDebut);
        periode.setDateFin(dateFin);

        return periodeRepository.save(periode);
    }

    public void supprimer(Long id) {
        if (!periodeRepository.existsById(id)) {
            throw new RuntimeException("Période introuvable");
        }
        periodeRepository.deleteById(id);
    }
}