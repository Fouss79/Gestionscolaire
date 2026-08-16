package com.saas.school.service;

import com.saas.school.dto.CoefficientMatiereRequest;
import com.saas.school.dto.CoefficientMatiereResponseDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoefficientMatiereService {

    private static final int COEFFICIENT_PAR_DEFAUT = 1;

    private final CoefficientMatiereRepository coefficientRepository;
    private final MatiereRepository matiereRepository;
    private final NiveauRepository niveauRepository;
    private final SerieRepository serieRepository;
    private final EcoleRepository ecoleRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;
    private final SousGroupeRepository sousGroupeRepository;
    private final  ClasseRepository classeRepository;
    private final AffectationEnseignantRepository affectationRepository; // ⚠️ à injecter


    public CoefficientMatiere creerOuModifier(CoefficientMatiereRequest request) {

        Matiere matiere = matiereRepository.findById(request.getMatiereId())
                .orElseThrow(() -> new RuntimeException("Matière introuvable"));

        Niveau niveau = niveauRepository.findById(request.getNiveauId())
                .orElseThrow(() -> new RuntimeException("Niveau introuvable"));

        Ecole ecole = ecoleRepository.findById(request.getEcoleId())
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        AnneeScolaire anneeScolaire = anneeScolaireRepository.findById(request.getAnneeScolaireId())
                .orElseThrow(() -> new RuntimeException("Année scolaire introuvable"));

        Serie serie = null;

        if (request.getSerieId() != null) {
            serie = serieRepository.findById(request.getSerieId())
                    .orElseThrow(() -> new RuntimeException("Série introuvable"));
        }

        Classe classe = null;

        if (request.getClasseId() != null) {
            classe = classeRepository.findById(request.getClasseId())
                    .orElseThrow(() -> new RuntimeException("Classe introuvable"));
        }

        SousGroupe sousGroupe = null;

        if (request.getSousGroupeId() != null) {
            sousGroupe = sousGroupeRepository.findById(request.getSousGroupeId())
                    .orElseThrow(() -> new RuntimeException("Sous-groupe introuvable"));
        }

        // =========================================================
        // RECHERCHE DU COEFFICIENT EXISTANT
        // =========================================================

        CoefficientMatiere existant =
                coefficientRepository.findCoefficient(
                        ecole.getId(),
                        matiere.getId(),
                        niveau.getId(),
                        serie != null ? serie.getId() : null,
                        anneeScolaire.getId(),
                        classe != null ? classe.getId() : null,
                        sousGroupe != null ? sousGroupe.getId() : null
                ).orElse(null);

        // =========================================================
        // CREATION OU MODIFICATION
        // =========================================================

        CoefficientMatiere coef =
                existant != null
                        ? existant
                        : new CoefficientMatiere();

        coef.setMatiere(matiere);
        coef.setNiveau(niveau);
        coef.setSerie(serie);
        coef.setAnneeScolaire(anneeScolaire);
        coef.setEcole(ecole);
        coef.setClasse(classe);
        coef.setSousGroupe(sousGroupe);

        coef.setCoefficient(request.getCoefficient());
        coef.setNombreHeuresParSemaine(
                request.getNombreHeuresParSemaine()
        );

        return coefficientRepository.save(coef);
    }
    public List<CoefficientMatiereResponseDTO> getByEcole(Long ecoleId) {
        return coefficientRepository.findByEcoleId(ecoleId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<CoefficientMatiereResponseDTO> getByEcoleEtAnnee(Long ecoleId, Long anneeScolaireId) {
        return coefficientRepository.findByEcoleIdAndAnneeScolaireId(ecoleId, anneeScolaireId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public void supprimer(Long id) {
        if (!coefficientRepository.existsById(id)) {
            throw new RuntimeException("Coefficient introuvable");
        }
        coefficientRepository.deleteById(id);
    }
    private CoefficientMatiereResponseDTO mapToDto(CoefficientMatiere c) {

        CoefficientMatiereResponseDTO dto = new CoefficientMatiereResponseDTO();

        dto.setId(c.getId());

        // =========================
        // MATIERE
        // =========================
        if (c.getMatiere() != null) {
            dto.setMatiereId(c.getMatiere().getId());
            dto.setMatiereNom(c.getMatiere().getNom());
        }

        // =========================
        // NIVEAU
        // =========================
        if (c.getNiveau() != null) {
            dto.setNiveauId(c.getNiveau().getId());
            dto.setNiveauNom(c.getNiveau().getNom());
        }

        // =========================
        // SERIE
        // =========================
        if (c.getSerie() != null) {
            dto.setSerieId(c.getSerie().getId());
            dto.setSerieNom(c.getSerie().getNom());
        } else {
            dto.setSerieId(null);
            dto.setSerieNom(null);
        }

        // =========================
        // CLASSE
        // =========================
        if (c.getClasse() != null) {
            dto.setClasseId(c.getClasse().getId());

            if (c.getClasse().getNomComplet() != null) {
                dto.setClasseNom(c.getClasse().getNomComplet());
            }
        }

        // =========================
        // SOUS-GROUPE
        // =========================
        if (c.getSousGroupe() != null) {
            dto.setSousGroupeId(c.getSousGroupe().getId());
            dto.setSousGroupeNom(c.getSousGroupe().getNom());
        } else {
            dto.setSousGroupeId(null);
            dto.setSousGroupeNom(null);
        }

        // =========================
        // COEFFICIENT
        // =========================
        dto.setCoefficient(c.getCoefficient());

        dto.setNombreHeuresParSemaine(
                c.getNombreHeuresParSemaine()
        );

        // =========================
        // ANNEE
        // =========================
        if (c.getAnneeScolaire() != null) {
            dto.setAnneeScolaireId(c.getAnneeScolaire().getId());
            dto.setAnneeScolaireNom(c.getAnneeScolaire().getNom());
        }

        List<String> noms = affectationRepository.findByCoefficientMatiereId(c.getId())
                .stream()
                .map(a -> a.getEnseignant().getPrenom() + " " + a.getEnseignant().getNom())
                .distinct()
                .toList();

        dto.setEnseignantsAffectes(noms);


        return dto;
    }

    public CoefficientMatiere getProgramme(Long ecoleId, Long matiereId, Long niveauId, Long serieId, Long anneeScolaireId) {

        List<CoefficientMatiere> candidats =
                coefficientRepository.findCandidats(ecoleId, matiereId, niveauId, serieId, anneeScolaireId);

        if (candidats.isEmpty()) {
            throw new RuntimeException("Programme introuvable");
        }

        return candidats.getFirst();
    }
    public List<CoefficientMatiereResponseDTO> getProgrammeParNiveau(
            Long ecoleId, Long anneeScolaireId, Long niveauId, Long serieId
    ) {

        List<CoefficientMatiere> candidats = coefficientRepository
                .findByEcoleIdAndAnneeScolaireIdAndNiveauId(ecoleId, anneeScolaireId, niveauId)
                .stream()
                .filter(c -> c.getSerie() == null || (serieId != null && c.getSerie().getId().equals(serieId)))
                .toList();

        return candidats.stream().map(this::mapToDto).toList();
    }

    public int getCoefficient(Long ecoleId, Long matiereId, Long niveauId, Long serieId, Long anneeScolaireId) {

        List<CoefficientMatiere> candidats =
                coefficientRepository.findCandidats(ecoleId, matiereId, niveauId, serieId, anneeScolaireId);

        if (candidats.isEmpty()) {
            return COEFFICIENT_PAR_DEFAUT;
        }


        return candidats.getFirst().getCoefficient();

    }


    public List<CoefficientMatiereResponseDTO> findProgrammesPourClasse(
            Long ecoleId,
            Long anneeScolaireId,
            Long niveauId,
            Long classeId
    ) {

        List<CoefficientMatiere> programmes =
                coefficientRepository.findProgrammesPourClasse(
                        ecoleId,
                        anneeScolaireId,
                        niveauId,
                        classeId
                );

        return programmes.stream()
                .map(this::mapToDto)
                .toList();
    }
}

