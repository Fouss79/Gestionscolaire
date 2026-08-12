package com.saas.school.service;

import com.saas.school.dto.*;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SousGroupeService {

    private final SousGroupeRepository sousGroupeRepository;
    private final ClasseRepository classeRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;
    private final EleveRepository eleveRepository;
    private final InscriptionRepository inscriptionRepository; // ⚠️ nouveau

    // =========================
    // 🟡 CRÉATION
    // =========================
    public SousGroupe creerSousGroupe(SousGroupeRequest request) {

        Classe classe = classeRepository.findById(request.getClasseId())
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        // ✅ Récupérer l'année scolaire
        AnneeScolaire anneeScolaire;
        if (request.getAnneeScolaireId() != null) {
            anneeScolaire = anneeScolaireRepository.findById(request.getAnneeScolaireId())
                    .orElseThrow(() -> new RuntimeException("Année scolaire introuvable"));
        } else {

            if (request.getAnneeScolaireId() != null) {
                anneeScolaire = anneeScolaireRepository.findById(request.getAnneeScolaireId())
                        .orElseThrow(() -> new RuntimeException("Année scolaire introuvable"));
            } else {
                anneeScolaire = anneeScolaireRepository.findByEcoleIdAndActiveTrue(classe.getEcole().getId())
                        .orElseThrow(() -> new RuntimeException("Aucune année scolaire active pour cette école"));
            } }

        // ✅ Vérifier l'unicité avec l'année
        if (sousGroupeRepository.existsByClasseIdAndNomAndAnneeScolaireId(
                classe.getId(), request.getNom(), anneeScolaire.getId())) {
            throw new RuntimeException("Un sous-groupe avec ce nom existe déjà dans cette classe pour cette année scolaire");
        }

        SousGroupe sousGroupe = new SousGroupe();
        sousGroupe.setNom(request.getNom());
        sousGroupe.setClasse(classe);
        sousGroupe.setEffectifMax(request.getEffectifMax());
        sousGroupe.setAnneeScolaire(anneeScolaire); // ✅ AJOUT

        if (request.getType() != null) {
            sousGroupe.setType(SousGroupe.TypeSousGroupe.valueOf(request.getType()));
        }

        return sousGroupeRepository.save(sousGroupe);
    }
    public List<EleveResponseDTO> getElevesSousGroupeAnneeActive(Long sousGroupeId) {

        SousGroupe sousGroupe = sousGroupeRepository.findById(sousGroupeId)
                .orElseThrow(() -> new RuntimeException("Sous-groupe introuvable"));

        Long anneeScolaireId = sousGroupe.getAnneeScolaire().getId();
        Long classeId = sousGroupe.getClasse().getId();

        // 🔥 Inscriptions réelles de la classe pour cette année — source des inscriptionId
        List<Inscription> inscriptions = inscriptionRepository
                .findByClasseIdAndAnneeScolaireId(classeId, anneeScolaireId);

        Map<Long, Long> eleveIdVersInscriptionId = inscriptions.stream()
                .collect(Collectors.toMap(i -> i.getEleve().getId(), Inscription::getId));

        List<Eleve> elevesDuSousGroupe = eleveRepository.findBySousGroupesId(sousGroupeId)
                .stream()
                .filter(e -> eleveIdVersInscriptionId.containsKey(e.getId()))
                .toList();

        return elevesDuSousGroupe.stream()
                .map(e -> {
                    EleveResponseDTO dto = new EleveResponseDTO();
                    dto.setId(eleveIdVersInscriptionId.get(e.getId())); // ⚠️ id = inscriptionId
                    dto.setNom(e.getNom());
                    dto.setPrenom(e.getPrenom());
                    dto.setSexe(e.getSexe());
                    dto.setNumeroMatricule(e.getMatricule());
                    dto.setSousGroupeIds(e.getSousGroupes().stream().map(SousGroupe::getId).toList());
                    return dto;
                })
                .toList();
    }
    // =========================
// 📊 STATISTIQUES DE TOUTES LES CLASSES D'UNE ÉCOLE (avec filtre année)
// =========================
    public List<ClasseStatsDTO> getAllClasseStats(Long ecoleId, Long anneeScolaireId) {
        // Récupérer toutes les classes de l'école
        List<Classe> classes = classeRepository.findByEcoleId(ecoleId);

        // Récupérer l'année scolaire
        AnneeScolaire annee = anneeScolaireRepository.findById(anneeScolaireId)
                .orElseThrow(() -> new RuntimeException("Année scolaire introuvable avec l'ID: " + anneeScolaireId));

        return classes.stream()
                .map(classe -> buildClasseStatsDTO(classe, annee))
                .collect(Collectors.toList());
    }

    // =========================
// 🔨 CONSTRUCTION DU DTO STATS
// =========================
    private ClasseStatsDTO buildClasseStatsDTO(Classe classe, AnneeScolaire annee) {

        ClasseStatsDTO dto = new ClasseStatsDTO();
        dto.setId(classe.getId());
        dto.setNomComplet(classe.getNomComplet());
        dto.setNiveauNom(classe.getNiveau() != null ? classe.getNiveau().getNom() : null);
        dto.setSerieNom(classe.getSerie() != null ? classe.getSerie().getNom() : null);
        dto.setGroupeNom(classe.getGroupe() != null ? classe.getGroupe().getNom() : null);

        // 🔥 Vraies inscriptions de la classe pour cette année — source de vérité pour les effectifs
        List<Inscription> inscriptions = inscriptionRepository
                .findByClasseIdAndAnneeScolaireId(classe.getId(), annee.getId());

        long nbValides = inscriptions.stream()
                .filter(i -> i.getStatut() == StatutInscription.VALIDE)
                .count();

        dto.setNbElevesInscrits(inscriptions.size());
        dto.setNbElevesValides((int) nbValides);

        // Sous-groupes — uniquement informatif, plus utilisé pour les totaux de la classe
        List<SousGroupe> sousGroupes = sousGroupeRepository.findByClasseIdAndAnneeScolaireId(
                classe.getId(), annee.getId()
        );

        Set<Long> idsElevesAnnee = inscriptions.stream()
                .map(i -> i.getEleve().getId())
                .collect(Collectors.toSet());

        List<SousGroupeStatsDTO> sousGroupesStats = sousGroupes.stream()
                .map(sg -> {
                    SousGroupeStatsDTO sgDTO = new SousGroupeStatsDTO();
                    sgDTO.setId(sg.getId());
                    sgDTO.setNom(sg.getNom());
                    sgDTO.setEffectifMax(sg.getEffectifMax());

                    long effectifActuel = eleveRepository.findBySousGroupesId(sg.getId())
                            .stream()
                            .filter(e -> idsElevesAnnee.contains(e.getId()))
                            .count();

                    sgDTO.setEffectifActuel((int) effectifActuel);
                    sgDTO.setEffectifTotal((int) effectifActuel); // ⚠️ voir remarque ci-dessous

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
    // 📥 LISTE PAR CLASSE
    // =========================
    public List<SousGroupeResponseDTO> getByClasse(Long classeId) {
        return sousGroupeRepository.findByClasseId(classeId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    // =========================
    // 📥 ÉLÈVES DE LA CLASSE, POUR L'ANNÉE ACTIVE (utile côté front pour proposer qui affecter)


    // =========================
    // ➕ AFFECTER UN ÉLÈVE
    // =========================

        // ... reste du code
        @Transactional
        public void affecterEleve(Long sousGroupeId, Long eleveId) {

            SousGroupe sousGroupe = sousGroupeRepository.findById(sousGroupeId)
                    .orElseThrow(() -> new RuntimeException("Sous-groupe introuvable"));

            Eleve eleve = eleveRepository.findById(eleveId)
                    .orElseThrow(() -> new RuntimeException("Élève introuvable"));

            // ✅ Vérifier que l'élève est inscrit dans la classe pour l'année du sous-groupe
            boolean appartientALaClasse = inscriptionRepository
                    .existsByEleveIdAndClasseIdAndAnneeScolaireId(
                            eleveId,
                            sousGroupe.getClasse().getId(),
                            sousGroupe.getAnneeScolaire().getId()
                    );

            if (!appartientALaClasse) {
                throw new RuntimeException("Cet élève n'est pas inscrit dans cette classe pour l'année scolaire "
                        + sousGroupe.getAnneeScolaire().getNom());
            }

            // ✅ Vérifier le type pour la même année
            if (sousGroupe.getType() != null) {
                boolean dejaDansUnSousGroupeDuMemeType = eleve.getSousGroupes().stream()
                        .anyMatch(sg ->
                                !sg.getId().equals(sousGroupe.getId())
                                        && sg.getType() == sousGroupe.getType()
                                        && sg.getAnneeScolaire().getId().equals(sousGroupe.getAnneeScolaire().getId())
                        );

                if (dejaDansUnSousGroupeDuMemeType) {
                    SousGroupe ancienSousGroupe = eleve.getSousGroupes().stream()
                            .filter(sg ->
                                    sg.getType() == sousGroupe.getType()
                                            && sg.getAnneeScolaire().getId().equals(sousGroupe.getAnneeScolaire().getId())
                            )
                            .findFirst()
                            .orElseThrow();

                    throw new RuntimeException(
                            "Cet élève est déjà affecté au sous-groupe \"" + ancienSousGroupe.getNom()
                                    + "\" du même type (" + sousGroupe.getType() + ") pour cette année scolaire. "
                                    + "Retirez-le d'abord de ce sous-groupe avant de l'ajouter à un autre."
                    );
                }
            }

            // ✅ Vérifier l'effectif maximum
            if (sousGroupe.getEffectifMax() != null && sousGroupe.getEffectifMax() > 0) {
                long effectifActuel = eleveRepository.countBySousGroupesId(sousGroupeId);
                if (effectifActuel >= sousGroupe.getEffectifMax()) {
                    throw new RuntimeException("Effectif maximum atteint pour ce sous-groupe");
                }
            }

            // ✅ AJOUTER L'ÉLÈVE AU SOUS-GROUPE
            eleve.getSousGroupes().add(sousGroupe);
            eleveRepository.save(eleve);
        }
        // =========================
    // ➖ RETIRER UN ÉLÈVE
    // =========================
    @Transactional
    public void retirerEleve(Long sousGroupeId, Long eleveId) {

        Eleve eleve = eleveRepository.findById(eleveId)
                .orElseThrow(() -> new RuntimeException("Élève introuvable"));

        eleve.getSousGroupes().removeIf(sg -> sg.getId().equals(sousGroupeId));
        eleveRepository.save(eleve);
    }

    // =========================
    // 🔁 MAPPING
    // =========================
    private SousGroupeResponseDTO mapToDto(SousGroupe sg) {

        // ✅ Récupérer les élèves de la classe pour l'année du sous-groupe
        Set<Long> idsElevesAnnee = inscriptionRepository
                .findByClasseIdAndAnneeScolaireId(
                        sg.getClasse().getId(),
                        sg.getAnneeScolaire().getId()
                )
                .stream()
                .map(i -> i.getEleve().getId())
                .collect(Collectors.toSet());

        // Élèves affectés au sous-groupe, filtrés pour ne garder que ceux de l'année
        List<Eleve> elevesDuSousGroupe = eleveRepository.findBySousGroupesId(sg.getId())
                .stream()
                .filter(e -> idsElevesAnnee.contains(e.getId()))
                .toList();

        SousGroupeResponseDTO dto = new SousGroupeResponseDTO();
        dto.setId(sg.getId());
        dto.setNom(sg.getNom());
        dto.setType(sg.getType() != null ? sg.getType().name() : null);
        dto.setClasseId(sg.getClasse().getId());
        dto.setClasseNom(sg.getClasse().getNomComplet());
        dto.setEffectifMax(sg.getEffectifMax());
        dto.setEffectifActuel(elevesDuSousGroupe.size());
        dto.setEleveIds(elevesDuSousGroupe.stream().map(Eleve::getId).toList());

        // ✅ Ajouter l'année dans le DTO
        if (sg.getAnneeScolaire() != null) {
            dto.setAnneeScolaireId(sg.getAnneeScolaire().getId());
            dto.setAnneeScolaireNom(sg.getAnneeScolaire().getNom());
        }

        return dto;
    }
    // Dans SousGroupeService.java - getElevesClasseAnneeActive
    public List<EleveResponseDTO> getElevesClasseAnneeActive(Long classeId) {
        Classe classe = classeRepository.findById(classeId)
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        // ✅ Toujours filtrer par école
        AnneeScolaire anneeActive = anneeScolaireRepository
                .findByEcoleIdAndActiveTrue(classe.getEcole().getId())
                .orElseThrow(() -> new RuntimeException("Aucune année scolaire active pour cette école"));

        return inscriptionRepository
                .findByClasseIdAndAnneeScolaireId(classeId, anneeActive.getId())
                .stream()
                .map(Inscription::getEleve)
                .distinct()
                .map(this::mapEleveToDto)
                .toList();
    }
    private EleveResponseDTO mapEleveToDto(Eleve e) {

        EleveResponseDTO dto = new EleveResponseDTO();
        dto.setId(e.getId());
        dto.setNom(e.getNom());
        dto.setPrenom(e.getPrenom());
        dto.setSexe(e.getSexe());
        dto.setNumeroMatricule(e.getMatricule());
        dto.setSousGroupeIds(
                e.getSousGroupes().stream().map(SousGroupe::getId).toList()
        );

        return dto;
    }
    // =========================
// 📊 NOMBRE D'ÉLÈVES PAR SOUS-GROUPE (année active uniquement)
// =========================
    public long getNombreElevesParSousGroupeAnneeActive(Long sousGroupeId) {
        SousGroupe sousGroupe = sousGroupeRepository.findById(sousGroupeId)
                .orElseThrow(() -> new RuntimeException("Sous-groupe introuvable"));

        // Récupérer les élèves de la classe pour l'année active
        Set<Long> idsElevesAnneeActive = inscriptionRepository
                .findByClasseIdAndAnneeScolaire_ActiveTrue(sousGroupe.getClasse().getId())
                .stream()
                .map(i -> i.getEleve().getId())
                .collect(Collectors.toSet());

        // Compter les élèves du sous-groupe qui sont dans l'année active
        return eleveRepository.findBySousGroupesId(sousGroupeId)
                .stream()
                .filter(e -> idsElevesAnneeActive.contains(e.getId()))
                .count();
    }

    // Dans SousGroupeService.java
    public List<SousGroupeResponseDTO> getByClasseAndAnnee(Long classeId, Long anneeScolaireId) {
        Classe classe = classeRepository.findById(classeId)
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        AnneeScolaire annee = anneeScolaireRepository.findById(anneeScolaireId)
                .orElseThrow(() -> new RuntimeException("Année scolaire introuvable"));

        // ✅ Vérifier que cette méthode existe dans le repository
        List<SousGroupe> sousGroupes = sousGroupeRepository.findByClasseAndAnneeScolaire(classe, annee);

        // ✅ Si aucun sous-groupe, retourner une liste vide (pas null)
        return sousGroupes.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
}