package com.saas.school.service;

import com.saas.school.dto.*;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClasseService {

    private final ClasseRepository classeRepository;
    private final NiveauRepository niveauRepository;
    private final SerieRepository serieRepository;
    private final GroupeRepository groupeRepository;
    private final EcoleRepository ecoleRepository;
    private final AbonnementService abonnementService;
    private final InscriptionRepository inscriptionRepository;
    private final SousGroupeRepository sousGroupeRepository;
    private final EleveRepository eleveRepository;
    private final SalleRepository salleRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;

    public Classe creerClasse(ClasseRequest request) {

        Niveau niveau = niveauRepository.findById(request.getNiveauId())
                .orElseThrow(() -> new RuntimeException("Niveau introuvable"));

        Ecole ecole = ecoleRepository.findById(request.getEcoleId())
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        if (!abonnementService.isActif(ecole)) {
            throw new RuntimeException("🚫 Abonnement expiré");
        }

        Serie serie = null;
        if (request.getSerieId() != null) {
            serie = serieRepository.findById(request.getSerieId())
                    .orElseThrow(() -> new RuntimeException("Série introuvable"));
        }

        Groupe groupe = null;
        if (request.getGroupeId() != null) {
            groupe = groupeRepository.findById(request.getGroupeId())
                    .orElseThrow(() -> new RuntimeException("Groupe introuvable"));
        }

        Salle salle = null;
        if (request.getSalleId() != null) {
            salle = salleRepository.findById(request.getSalleId())
                    .orElseThrow(() -> new RuntimeException("Salle introuvable"));
        }

        boolean existeDeja = classeRepository.existsCombinaison(
                ecole.getId(), niveau.getId(),
                serie != null ? serie.getId() : null,
                groupe != null ? groupe.getId() : null
        );

        if (existeDeja) {
            throw new RuntimeException(
                    "Une classe existe déjà avec cette combinaison niveau/série/groupe pour cette école"
            );
        }

        Classe classe = new Classe();
        classe.setNiveau(niveau);
        classe.setSerie(serie);
        classe.setGroupe(groupe);
        classe.setSalle(salle);
        classe.setEcole(ecole);

        return classeRepository.save(classe);
    }

    // 🔥 Une seule méthode modifier, salle incluse (toujours), plus de doublon
    public Classe modifier(Long id, Long niveauId, Long serieId, Long groupeId, Long salleId) {

        Classe classe = classeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        Niveau niveau = niveauRepository.findById(niveauId)
                .orElseThrow(() -> new RuntimeException("Niveau introuvable"));
        classe.setNiveau(niveau);

        Serie serie = serieRepository.findById(serieId)
                .orElseThrow(() -> new RuntimeException("Série introuvable"));
        classe.setSerie(serie);

        if (groupeId != null) {
            Groupe groupe = groupeRepository.findById(groupeId)
                    .orElseThrow(() -> new RuntimeException("Groupe introuvable"));
            classe.setGroupe(groupe);
        } else {
            classe.setGroupe(null);
        }

        if (salleId != null) {
            Salle salle = salleRepository.findById(salleId)
                    .orElseThrow(() -> new RuntimeException("Salle introuvable"));
            classe.setSalle(salle);
        } else {
            classe.setSalle(null);
        }

        return classeRepository.save(classe);
    }

    public void supprimer(Long id) {
        if (!classeRepository.existsById(id)) {
            throw new RuntimeException("Classe introuvable");
        }
        classeRepository.deleteById(id);
    }

    public List<Classe> getClasseByEcole(Long ecoleId) {
        return classeRepository.findByEcoleId(ecoleId);
    }

    // 🔥 SEULE méthode de stats conservée — basée sur les vraies inscriptions, pas de double comptage
    public List<ClasseResponseDTO> getByEcoleAvecStats(Long ecoleId) {
        return classeRepository.findByEcoleId(ecoleId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    private ClasseResponseDTO mapToDto(Classe classe) {

        List<Inscription> inscriptions = inscriptionRepository
                .findByClasseIdAndAnneeScolaire_ActiveTrue(classe.getId());

        long nbValides = inscriptions.stream()
                .filter(i -> i.getStatut() == StatutInscription.VALIDE)
                .count();

        ClasseResponseDTO dto = new ClasseResponseDTO();
        dto.setId(classe.getId());
        dto.setNiveauNom(classe.getNomNiveau());
        dto.setSerieNom(classe.getSerie() != null ? classe.getSerie().getNom() : null);
        dto.setGroupeNom(classe.getGroupe() != null ? classe.getGroupe().getNom() : null);
        dto.setNomComplet(classe.getNomComplet());
        dto.setEcoleId(classe.getEcole().getId());
        dto.setNbElevesInscrits(inscriptions.size());
        dto.setNbElevesValides((int) nbValides);

        // Sous-groupes de cette classe — filtrés sur l'année active pour cohérence
        List<SousGroupeResumeDTO> sousGroupesDto = sousGroupeRepository
                .findByClasseId(classe.getId())
                .stream()
                .filter(sg -> sg.getAnneeScolaire() == null || sg.getAnneeScolaire().isActive())
                .map(sg -> {
                    SousGroupeResumeDTO resumeDto = new SousGroupeResumeDTO();
                    resumeDto.setId(sg.getId());
                    resumeDto.setNom(sg.getNom());
                    resumeDto.setType(sg.getType() != null ? sg.getType().name() : null);
                    resumeDto.setEffectifActuel((int) eleveRepository.countBySousGroupesId(sg.getId()));
                    resumeDto.setEffectifMax(sg.getEffectifMax());
                    return resumeDto;
                })
                .toList();

        dto.setSousGroupes(sousGroupesDto);

        return dto;
    }

    // =========================
    // 📊 STATISTIQUES DE TOUTES LES CLASSES D'UNE ÉCOLE (NOUVELLE MÉTHODE)
    // =========================
    // Dans ClasseService.java

    // Dans ClasseService.java - getClassesStats
    public List<ClasseStatsDTO> getClassesStats(Long ecoleId) {
        // ✅ Toujours filtrer par école
        AnneeScolaire anneeActive = anneeScolaireRepository
                .findByEcoleIdAndActiveTrue(ecoleId)
                .orElseThrow(() -> new RuntimeException("Aucune année scolaire active pour cette école"));

        List<Classe> classes = classeRepository.findByEcoleId(ecoleId);

        return classes.stream()
                .map(classe -> buildClasseStatsDTO(classe, anneeActive))
                .collect(Collectors.toList());
    }
    private ClasseStatsDTO buildClasseStatsDTO(Classe classe, AnneeScolaire anneeActive) {
        ClasseStatsDTO dto = new ClasseStatsDTO();
        dto.setId(classe.getId());
        dto.setNomComplet(classe.getNomComplet());
        dto.setCycleNom(
                classe.getNiveau() != null
                        && classe.getNiveau().getCycle() != null
                        ? classe.getNiveau().getCycle().getNom()
                        : null
        );
        dto.setNiveauNom(classe.getNiveau() != null ? classe.getNiveau().getNom() : null);
        dto.setSerieNom(classe.getSerie() != null ? classe.getSerie().getNom() : null);
        dto.setGroupeNom(classe.getGroupe() != null ? classe.getGroupe().getNom() : null);

        // ✅ Récupérer les inscriptions pour l'année active
        List<Inscription> inscriptions = inscriptionRepository
                .findByClasseIdAndAnneeScolaireId(classe.getId(), anneeActive.getId());

        long nbValides = inscriptions.stream()
                .filter(i -> i.getStatut() == StatutInscription.VALIDE)
                .count();

        dto.setNbElevesInscrits(inscriptions.size());
        dto.setNbElevesValides((int) nbValides);

        // Récupérer les IDs des élèves
        Set<Long> idsElevesAnneeActive = inscriptions.stream()
                .map(i -> i.getEleve().getId())
                .collect(Collectors.toSet());

        // ✅ Récupérer uniquement les sous-groupes de l'année active
        List<SousGroupe> sousGroupes = sousGroupeRepository
                .findByClasseIdAndAnneeScolaireId(classe.getId(), anneeActive.getId());

        List<SousGroupeStatsDTO> sousGroupesStats = sousGroupes.stream()
                .map(sg -> {
                    SousGroupeStatsDTO sgDTO = new SousGroupeStatsDTO();
                    sgDTO.setId(sg.getId());
                    sgDTO.setNom(sg.getNom());
                    sgDTO.setEffectifMax(sg.getEffectifMax());

                    long effectifActuel = eleveRepository.findBySousGroupesId(sg.getId())
                            .stream()
                            .filter(e -> idsElevesAnneeActive.contains(e.getId()))
                            .count();

                    sgDTO.setEffectifActuel((int) effectifActuel);
                    sgDTO.setEffectifTotal((int) effectifActuel);

                    if (sg.getAnneeScolaire() != null) {
                        sgDTO.setAnneeScolaireId(sg.getAnneeScolaire().getId());
                        sgDTO.setAnneeScolaireLibelle(sg.getAnneeScolaire().getNom());
                    }

                    return sgDTO;
                })
                .collect(Collectors.toList());

        dto.setSousGroupes(sousGroupesStats);

        return dto;
    }
    // =========================
    // 🔨 CONSTRUCTION DU DTO STATS
    // =========================
    private ClasseStatsDTO buildClasseStatsDTO(Classe classe) {
        ClasseStatsDTO dto = new ClasseStatsDTO();
        dto.setId(classe.getId());
        dto.setNomComplet(classe.getNomComplet());
        dto.setNiveauNom(classe.getNiveau() != null ? classe.getNiveau().getNom() : null);
        dto.setSerieNom(classe.getSerie() != null ? classe.getSerie().getNom() : null);
        dto.setGroupeNom(classe.getGroupe() != null ? classe.getGroupe().getNom() : null);

        // Récupérer les inscriptions actives pour cette classe
        List<Inscription> inscriptions = inscriptionRepository
                .findByClasseIdAndAnneeScolaire_ActiveTrue(classe.getId());

        // Compter les élèves validés
        long nbValides = inscriptions.stream()
                .filter(i -> i.getStatut() == StatutInscription.VALIDE)
                .count();

        dto.setNbElevesInscrits(inscriptions.size());
        dto.setNbElevesValides((int) nbValides);

        // Récupérer les IDs des élèves de la classe pour l'année active
        Set<Long> idsElevesAnneeActive = inscriptions.stream()
                .map(i -> i.getEleve().getId())
                .collect(Collectors.toSet());

        // Récupérer les sous-groupes de la classe
        List<SousGroupe> sousGroupes = sousGroupeRepository.findByClasseId(classe.getId());

        // Filtrer les sous-groupes par année active
        List<SousGroupeStatsDTO> sousGroupesStats = sousGroupes.stream()
                .filter(sg -> sg.getAnneeScolaire() == null || sg.getAnneeScolaire().isActive())
                .map(sg -> {
                    SousGroupeStatsDTO sgDTO = new SousGroupeStatsDTO();
                    sgDTO.setId(sg.getId());
                    sgDTO.setNom(sg.getNom());
                    sgDTO.setEffectifMax(sg.getEffectifMax());

                    // Compter les élèves du sous-groupe qui sont dans l'année active
                    long effectifActuel = eleveRepository.findBySousGroupesId(sg.getId())
                            .stream()
                            .filter(e -> idsElevesAnneeActive.contains(e.getId()))
                            .count();

                    sgDTO.setEffectifActuel((int) effectifActuel);
                    sgDTO.setEffectifTotal((int) effectifActuel);

                    if (sg.getAnneeScolaire() != null) {
                        sgDTO.setAnneeScolaireId(sg.getAnneeScolaire().getId());
                        sgDTO.setAnneeScolaireLibelle(sg.getAnneeScolaire().getNom());
                    }

                    return sgDTO;
                })
                .collect(Collectors.toList());

        dto.setSousGroupes(sousGroupesStats);

        return dto;
    }
    public List<Classe> getClassesByEcoleAndNiveau(Long ecoleId, Long niveauId) {
        return classeRepository.findByEcoleIdAndNiveauId(ecoleId, niveauId);
    }
}