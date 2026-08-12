package com.saas.school.service;

import com.saas.school.dto.NoteRequest;
import com.saas.school.dto.NoteResponseDTO;
import com.saas.school.dto.NotesEnMasseRequest;
import com.saas.school.dto.NotesEnMasseResponseDTO;
import com.saas.school.entity.CoefficientMatiere;
import com.saas.school.entity.Inscription;
import com.saas.school.entity.Note;
import com.saas.school.entity.SousGroupe;
import com.saas.school.exception.IncompatibleClasseException;
import com.saas.school.exception.IncompatibleSousGroupeException;
import com.saas.school.exception.ResourceNotFoundException;
import com.saas.school.repository.CoefficientMatiereRepository;
import com.saas.school.repository.InscriptionRepository;
import com.saas.school.repository.NoteRepository;
import com.saas.school.repository.SousGroupeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {

    private final NoteRepository noteRepository;
    private final InscriptionRepository inscriptionRepository;
    private final CoefficientMatiereRepository coefficientMatiereRepository;
    private final SousGroupeRepository sousGroupeRepository;

    @Transactional
    public Note creerOuMettreAJour(NoteRequest request) {

        Inscription inscription = inscriptionRepository
                .findById(request.getInscriptionId())
                .orElseThrow(() ->
                        new RuntimeException("Inscription introuvable"));

        CoefficientMatiere programme = coefficientMatiereRepository
                .findById(request.getCoefficientMatiereId())
                .orElseThrow(() ->
                        new RuntimeException("Ligne de programme introuvable"));

        // 🔥 Le programme est la source de vérité
        SousGroupe sousGroupe = programme.getSousGroupe();

        // Vérification classe
        verifierCompatibiliteClasse(inscription, programme);

        // Vérification sous-groupe
        verifierCompatibiliteSousGroupe(inscription, programme);

        Note note;

        if (sousGroupe != null) {

            note = noteRepository
                    .findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeId(
                            inscription.getId(),
                            programme.getId(),
                            request.getPeriode(),
                            sousGroupe.getId()
                    )
                    .orElse(new Note());

        } else {

            note = noteRepository
                    .findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeIsNull(
                            inscription.getId(),
                            programme.getId(),
                            request.getPeriode()
                    )
                    .orElse(new Note());
        }

        note.setInscription(inscription);
        note.setCoefficientMatiere(programme);

        note.setClasse(inscription.getClasse());
        note.setEleve(inscription.getEleve());
        note.setAnneeScolaire(inscription.getAnneeScolaire());
        note.setMatiere(programme.getMatiere());
        note.setCoeff(programme.getCoefficient());

        // 🔥 sous-groupe du programme
        note.setSousGroupe(sousGroupe);

        note.setPeriode(request.getPeriode());
        note.setNClass(request.getNClass());
        note.setNExem(request.getNExem());

        return noteRepository.save(note);
    }
    /**
     * Enregistre les notes pour plusieurs élèves d'un coup
     *
     *
     * @return Response avec le nombre de notes enregistrées et les détails
     * @throws ResourceNotFoundException Si le programme ou une inscription n'est pas trouvé
     * @throws IncompatibleClasseException Si la classe ne correspond pas
     * @throws IncompatibleSousGroupeException Si l'élève n'appartient pas au sous-groupe
     */


// Avant : private NoteResponseDTO mapToDto(Note n) { ... }
// Après :
    public NoteResponseDTO toDto(Note n) {
        CoefficientMatiere programme = n.getCoefficientMatiere();
        double moyenne = calculerMoyenneNote(n);

        NoteResponseDTO dto = new NoteResponseDTO();
        dto.setId(n.getId());
        dto.setInscriptionId(n.getInscription().getId());
        dto.setEleveNom(n.getInscription().getEleve().getNom());
        dto.setElevePrenom(n.getInscription().getEleve().getPrenom());
        dto.setMatiereId(programme.getMatiere().getId());
        dto.setMatiereNom(programme.getMatiere().getNom());
        dto.setCoeff(programme.getCoefficient());
        dto.setPeriode(n.getPeriode());
        dto.setMoyenne(moyenne);
        dto.setPoints(moyenne * programme.getCoefficient());
        dto.setNClass(n.getNClass());
        dto.setNExem(n.getNExem());

        if (n.getSousGroupe() != null) {
            dto.setSousGroupeId(n.getSousGroupe().getId());
            dto.setSousGroupeNom(n.getSousGroupe().getNom());
        }

        return dto;
    }


    @Transactional
    public List<Note> enregistrerEnMasse(NotesEnMasseRequest request) {

        CoefficientMatiere programme = coefficientMatiereRepository
                .findById(request.getCoefficientMatiereId())
                .orElseThrow(() ->
                        new RuntimeException("Ligne de programme introuvable"));

        // 🔥 Le sous-groupe vient du PROGRAMME
        SousGroupe sousGroupe = programme.getSousGroupe();

        log.info(
                "📝 Enregistrement notes - programme={}, matière={}, sousGroupe={}",
                programme.getId(),
                programme.getMatiere().getNom(),
                sousGroupe != null ? sousGroupe.getId() : null
        );

        List<Note> resultats = new ArrayList<>();

        for (NotesEnMasseRequest.NoteEleveDTO ne : request.getNotes()) {

            if (ne.getNClass() == null && ne.getNExem() == null) {
                continue;
            }

            Inscription inscription = inscriptionRepository
                    .findById(ne.getInscriptionId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Inscription introuvable : "
                                            + ne.getInscriptionId()
                            ));

            // Vérification classe
            if (programme.getClasse() != null
                    && inscription.getClasse() != null
                    && !programme.getClasse().getId()
                    .equals(inscription.getClasse().getId())) {

                throw new IncompatibleClasseException(
                        "L'élève " +
                                inscription.getEleve().getNom() +
                                " " +
                                inscription.getEleve().getPrenom() +
                                " n'appartient pas à la classe du programme"
                );
            }

            // 🔥 Vérification sous-groupe
            if (sousGroupe != null
                    && !estCompatibleAvecSousGroupe(inscription, programme)) {

                throw new IncompatibleSousGroupeException(
                        "L'élève " +
                                inscription.getEleve().getNom() +
                                " " +
                                inscription.getEleve().getPrenom() +
                                " n'appartient pas au sous-groupe " +
                                sousGroupe.getNom()
                );
            }

            // 🔥 Recherche avec le sous-groupe DU PROGRAMME
            Note note;

            if (sousGroupe != null) {

                note = noteRepository
                        .findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeId(
                                inscription.getId(),
                                programme.getId(),
                                request.getPeriode(),
                                sousGroupe.getId()
                        )
                        .orElse(new Note());

            } else {

                note = noteRepository
                        .findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeIsNull(
                                inscription.getId(),
                                programme.getId(),
                                request.getPeriode()
                        )
                        .orElse(new Note());
            }

            // ============================
            // 🔥 Champs de la note
            // ============================

            note.setInscription(inscription);
            note.setCoefficientMatiere(programme);

            note.setClasse(inscription.getClasse());
            note.setEleve(inscription.getEleve());
            note.setAnneeScolaire(inscription.getAnneeScolaire());
            note.setMatiere(programme.getMatiere());
            note.setCoeff(programme.getCoefficient());

            // 🔥 IMPORTANT
            note.setSousGroupe(sousGroupe);

            note.setPeriode(request.getPeriode());
            note.setNClass(ne.getNClass());
            note.setNExem(ne.getNExem());

            resultats.add(noteRepository.save(note));
        }

        return resultats;
    }
    // =========================================================
// MÉTHODES UTILITAIRES EXTRACTEES
// =========================================================

    /**
     * Vérifie que la classe demandée correspond au programme
     */
    private void verifierClasseProgramme(Long classeId, CoefficientMatiere programme) {
        if (classeId != null
                && programme.getClasse() != null
                && !programme.getClasse().getId().equals(classeId)) {
            throw new IncompatibleClasseException(
                    "Cette matière n'est pas programmée pour la classe " + classeId
            );
        }
    }

    /**
     * Vérifie si une note est vide
     */
    private boolean estNoteVide(NotesEnMasseRequest.NoteEleveDTO note) {
        return note.getNClass() == null && note.getNExem() == null;
    }

    /**
     * Trouve une note existante ou en crée une nouvelle
     */
    private Note findOrCreateNote(Inscription inscription,
                                  CoefficientMatiere programme,
                                  SousGroupe sousGroupe,
                                  String periode) {

        if (sousGroupe != null) {
            return noteRepository
                    .findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeId(
                            inscription.getId(),
                            programme.getId(),
                            periode,
                            sousGroupe.getId()
                    )
                    .orElse(new Note());
        } else {
            return noteRepository
                    .findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeIsNull(
                            inscription.getId(),
                            programme.getId(),
                            periode
                    )
                    .orElse(new Note());
        }
    }

    /**
     * Met à jour les champs d'une note
     */
    private void mettreAJourNote(Inscription inscription,
                                 CoefficientMatiere programme,
                                 Note note,
                                 NotesEnMasseRequest.NoteEleveDTO noteEleve,
                                 String periode) {
        // Champs communs
        note.setInscription(inscription);
        note.setCoefficientMatiere(programme);
        note.setClasse(inscription.getClasse());
        note.setEleve(inscription.getEleve());
        note.setAnneeScolaire(inscription.getAnneeScolaire());
        note.setMatiere(programme.getMatiere());
        note.setCoeff(programme.getCoefficient());

        // Champs spécifiques
        note.setSousGroupe(programme.getSousGroupe());
        note.setPeriode(periode);
        note.setNClass(noteEleve.getNClass());
        note.setNExem(noteEleve.getNExem());
    }
    // 🔥 Choisit automatiquement la bonne source selon si un sous-groupe est précisé
    public List<NoteResponseDTO> getByClasseMatierePeriode(
            Long classeId,
            Long coefficientMatiereId,
            String periode,
            Long sousGroupeId
    ) {

        log.info("📚 Chargement notes");
        log.info("➡️ classeId={}", classeId);
        log.info("➡️ coefficientMatiereId={}", coefficientMatiereId);
        log.info("➡️ periode={}", periode);
        log.info("➡️ sousGroupeId={}", sousGroupeId);

        List<Note> notes;

        if (sousGroupeId != null) {

            notes = noteRepository.findNotesClasseMatierePeriodeSousGroupe(
                    classeId,
                    coefficientMatiereId,
                    periode,
                    sousGroupeId
            );

        } else {

            notes = noteRepository.findNotesClasseMatierePeriodeSansSousGroupe(
                    classeId,
                    coefficientMatiereId,
                    periode
            );
        }

        log.info("📥 Nombre de notes trouvées : {}", notes.size());

        notes.forEach(n ->
                log.info(
                        "📝 Note id={} inscription={} coefficient={} sousGroupe={} periode={}",
                        n.getId(),
                        n.getInscription().getId(),
                        n.getCoefficientMatiere().getId(),
                        n.getSousGroupe() != null
                                ? n.getSousGroupe().getId()
                                : null,
                        n.getPeriode()
                )
        );

        return notes.stream()
                .map(this::toDto)
                .toList();
    }
    public List<NoteResponseDTO> getByInscriptionEtPeriode(Long inscriptionId, String periode) {
        return noteRepository.findByInscriptionIdAndPeriode(inscriptionId, periode)
                .stream().map(this::toDto).toList();
    }

    public void supprimer(Long id) {
        if (!noteRepository.existsById(id)) throw new RuntimeException("Note introuvable");
        noteRepository.deleteById(id);
    }

    public Double calculMoyennePeriode(Long inscriptionId, String periode) {
        List<Note> notes = noteRepository.findByInscriptionIdAndPeriode(inscriptionId, periode);
        if (notes.isEmpty()) return null;

        double sommeCoeff = notes.stream().mapToDouble(n -> n.getCoefficientMatiere().getCoefficient()).sum();
        if (sommeCoeff == 0) return null;

        double sommePoints = notes.stream()
                .mapToDouble(n -> calculerMoyenneNote(n) * n.getCoefficientMatiere().getCoefficient())
                .sum();

        return sommePoints / sommeCoeff;
    }

    private double calculerMoyenneNote(Note n) {
        double nClass = n.getNClass() != null ? n.getNClass() : 0;
        double nExem = n.getNExem() != null ? n.getNExem() : 0;
        return (nClass + nExem * 2) / 3;
    }

    private NoteResponseDTO mapToDto(Note n) {
        CoefficientMatiere programme = n.getCoefficientMatiere();
        double moyenne = calculerMoyenneNote(n);

        NoteResponseDTO dto = new NoteResponseDTO();
        dto.setId(n.getId());
        dto.setInscriptionId(n.getInscription().getId());
        dto.setEleveNom(n.getInscription().getEleve().getNom());
        dto.setElevePrenom(n.getInscription().getEleve().getPrenom());
        dto.setMatiereId(programme.getMatiere().getId());
        dto.setMatiereNom(programme.getMatiere().getNom());
        dto.setCoeff(programme.getCoefficient());
        dto.setPeriode(n.getPeriode());
        dto.setMoyenne(moyenne);
        dto.setPoints(moyenne * programme.getCoefficient());
        dto.setNClass(n.getNClass());
        dto.setNExem(n.getNExem());

        if (n.getSousGroupe() != null) {
            dto.setSousGroupeId(n.getSousGroupe().getId());
            dto.setSousGroupeNom(n.getSousGroupe().getNom());
        }

        return dto;
    }

    private static final double POIDS_CONTROLE_CLASS = 1.0;
    private static final double POIDS_EXAMEN = 2.0;
    private static final int DIVISEUR_MOYENNE_NOTE = 3;


    /**
     * Calcule la moyenne annuelle d'un élève
     */
    public Double calculMoyenneAnnuelle(Long eleveId, Long anneeScolaireId) {
        List<Note> notes = noteRepository.findByEleveIdAndAnneeScolaireId(eleveId, anneeScolaireId);
        return calculerMoyenneAvecFiltreSousGroupe(notes);
    }

    // =========================================================
    // MÉTHODES PRIVÉES UTILITAIRES
    // =========================================================

    /**
     * Calcule la moyenne en filtrant les notes par compatibilité sous-groupe
     */
    private Double calculerMoyenneAvecFiltreSousGroupe(List<Note> notes) {
        if (notes.isEmpty()) {
            return null;
        }

        // Filtrer les notes compatibles avec les sous-groupes de l'élève
        List<Note> notesFiltrees = notes.stream()
                .filter(n -> estCompatibleAvecSousGroupe(n.getInscription(), n.getCoefficientMatiere()))
                .toList();

        return calculerMoyennePonderee(notesFiltrees);
    }

    /**
     * Calcule une moyenne pondérée à partir d'une liste de notes
     */
    private Double calculerMoyennePonderee(List<Note> notes) {
        if (notes.isEmpty()) {
            return null;
        }

        double sommeCoeff = notes.stream()
                .mapToDouble(n -> n.getCoefficientMatiere().getCoefficient())
                .sum();

        if (sommeCoeff == 0) {
            return null;
        }

        double sommePoints = notes.stream()
                .mapToDouble(n -> calculerMoyenneNote(n) * n.getCoefficientMatiere().getCoefficient())
                .sum();

        return sommePoints / sommeCoeff;
    }

    /**
     * Calcule la moyenne d'une note (contrôle + examen)
     */

    /**
     * Vérifie la compatibilité d'un élève avec le sous-groupe d'un coefficient
     */
    private void verifierCompatibiliteSousGroupe(Inscription inscription, CoefficientMatiere programme) {
        if (!estCompatibleAvecSousGroupe(inscription, programme)) {
            String nomSousGroupe = programme.getSousGroupe() != null
                    ? programme.getSousGroupe().getNom()
                    : "inconnu";

            throw new IncompatibleSousGroupeException(
                    String.format("L'élève %s %s n'appartient pas au sous-groupe %s",
                            inscription.getEleve().getNom(),
                            inscription.getEleve().getPrenom(),
                            nomSousGroupe)
            );
        }
    }

    /**
     * Vérifie la compatibilité d'un élève avec la classe d'un coefficient
     */
    private void verifierCompatibiliteClasse(Inscription inscription, CoefficientMatiere programme) {
        if (!estCompatibleAvecClasse(inscription, programme)) {
            throw new IncompatibleClasseException(
                    String.format("Cette matière n'est pas programmée pour la classe %s",
                            inscription.getClasse().getNomComplet())
            );
        }
    }

    /**
     * Vérifie si l'élève est compatible avec le sous-groupe
     */
    private boolean estCompatibleAvecSousGroupe(Inscription inscription, CoefficientMatiere programme) {
        // Pas de sous-groupe → compatible avec tous
        if (programme.getSousGroupe() == null) {
            return true;
        }

        // L'élève n'a pas de sous-groupes → incompatible
        if (inscription.getEleve().getSousGroupes() == null ||
                inscription.getEleve().getSousGroupes().isEmpty()) {
            return false;
        }

        Long sousGroupeId = programme.getSousGroupe().getId();
        return inscription.getEleve().getSousGroupes().stream()
                .anyMatch(sg -> sg != null && sg.getId() != null && sg.getId().equals(sousGroupeId));
    }

    /**
     * Vérifie si l'élève est compatible avec la classe
     */
    private boolean estCompatibleAvecClasse(Inscription inscription, CoefficientMatiere programme) {
        return programme.getClasse() == null ||
                inscription.getClasse() == null ||
                programme.getClasse().getId().equals(inscription.getClasse().getId());
    }

    /**
     * Récupère une inscription ou lève une exception
     */
    private Inscription getInscription(Long id) {
        return inscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription introuvable: " + id));
    }

    /**
     * Récupère un coefficient matière ou lève une exception
     */
    private CoefficientMatiere getCoefficientMatiere(Long id) {
        return coefficientMatiereRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ligne de programme introuvable: " + id));
    }

    /**
     * Recherche une note existante ou en crée une nouvelle
     */
    private Note findOrCreateNote(Inscription inscription, CoefficientMatiere programme, String periode) {
        return noteRepository
                .findByInscriptionIdAndCoefficientMatiereIdAndPeriode(
                        inscription.getId(), programme.getId(), periode)
                .orElse(new Note());
    }

    /**
     * Met à jour les champs d'une note
     */
    private void mettreAJourNote(Note note, Inscription inscription, CoefficientMatiere programme,
                                 NoteRequest request) {
        setNoteFields(note, inscription, programme);
        note.setPeriode(request.getPeriode());
        note.setNClass(request.getNClass());
        note.setNExem(request.getNExem());
    }

    /**
     * Met à jour les champs d'une note pour l'enregistrement groupé
     */
    private void mettreAJourNoteEleve(Note note, Inscription inscription, CoefficientMatiere programme,
                                      NotesEnMasseRequest.NoteEleveDTO noteEleve, String periode) {
        setNoteFields(note, inscription, programme);
        note.setPeriode(periode);
        note.setNClass(noteEleve.getNClass());
        note.setNExem(noteEleve.getNExem());
    }

    /**
     * Définit les champs communs d'une note
     */
    private void setNoteFields(Note note, Inscription inscription, CoefficientMatiere programme) {
        note.setInscription(inscription);
        note.setCoefficientMatiere(programme);
        note.setClasse(inscription.getClasse());
        note.setEleve(inscription.getEleve());
        note.setAnneeScolaire(inscription.getAnneeScolaire());
        note.setMatiere(programme.getMatiere());
        note.setCoeff(programme.getCoefficient());
    }

    // =========================================================
    // 🔁 MAPPING VERS DTO
    // =========================================================

    /**
     * Convertit une Note en NoteResponseDTO
     */






}