package com.saas.school.service;

import com.saas.school.dto.NotesPrimaireDtos.*;
import com.saas.school.entity.Classe;
import com.saas.school.entity.CoefficientMatiere;
import com.saas.school.entity.Inscription;
import com.saas.school.entity.Note;
import com.saas.school.exception.IncompatibleClasseException;
import com.saas.school.exception.ResourceNotFoundException;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.ClasseRepository;
import com.saas.school.repository.CoefficientMatiereRepository;
import com.saas.school.repository.InscriptionRepository;
import com.saas.school.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * =========================================================
 * 🎒 SERVICE NOTES PRIMAIRE (fonctionnalité isolée)
 * =========================================================
 *
 * IMPORTANT :
 * - Ne touche à aucune méthode de NoteService.
 * - Réutilise l'entité Note existante (pas de NotePrimaire).
 * - Note primaire = toujours nClass, sur 0 à 10, nExem = null, sousGroupe = null.
 * - "periode" (colonne existante de Note) porte ici le nom du mois
 *   (SEPTEMBRE, OCTOBRE, ...).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotesPrimaireService {

    private static final double NOTE_MIN = 0d;
    private static final double NOTE_MAX = 10d;

    private final ClasseRepository classeRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;
    private final InscriptionRepository inscriptionRepository;
    private final CoefficientMatiereRepository coefficientMatiereRepository;
    private final NoteRepository noteRepository;

    // =========================================================
    // 📥 CHARGEMENT (élèves × matières × notes du mois)
    // =========================================================

    @Transactional(readOnly = true)
    public DonneesPrimaireDto charger(Long classeId, Long anneeId, String mois) {

        Classe classe = getClasse(classeId);
        getAnneeScolaireOuThrow(anneeId);

        Long ecoleId = classe.getEcole().getId();
        Long niveauId = classe.getNiveau().getId();

        // 1) Élèves : inscriptions valides de cette classe pour cette année.
        //    Réutilisation de la méthode existante (mêmes critères que
        //    "findActifsByClasseAndAnnee" demandé : classe + année + statut VALIDE),
        //    afin de ne pas dupliquer une méthode au même effet.
        List<Inscription> inscriptions =
                inscriptionRepository.findElevesValidesPourReleve(classeId, anneeId);

        List<ElevePrimaireDto> eleves = inscriptions.stream()
                .map(this::toEleveDto)
                .toList();

        // 2) Matières : programme de la classe, sans sous-groupe (primaire = pas de sous-groupes),
        //    triées par nom de matière puis id (la colonne "ordreAffichage" n'existe pas
        //    dans le modèle actuel : voir note ci-dessous).
        List<CoefficientMatiere> programme = coefficientMatiereRepository
                .findProgrammesPourClasse(ecoleId, anneeId, niveauId, classeId)
                .stream()
                .filter(cm -> cm.getSousGroupe() == null)
                .sorted(Comparator
                        .comparing((CoefficientMatiere cm) -> cm.getMatiere().getNom())
                        .thenComparing(CoefficientMatiere::getId))
                .toList();

        List<MatierePrimaireDto> matieres = toMatiereDtos(programme);

        // 3) Notes déjà saisies pour ce mois / cette année.
        List<Long> inscriptionIds = inscriptions.stream().map(Inscription::getId).toList();

        List<NotePrimaireDto> notes = inscriptionIds.isEmpty()
                ? List.of()
                : noteRepository.findNotesPrimaire(inscriptionIds, anneeId, mois).stream()
                .map(this::toNoteDto)
                .toList();

        return new DonneesPrimaireDto(eleves, matieres, notes);
    }

    // =========================================================
    // 💾 ENREGISTREMENT
    // =========================================================

    @Transactional
    public void enregistrer(EnregistrementNotesPrimaireRequest request) {

        Classe classe = getClasse(request.classeId());
        getAnneeScolaireOuThrow(request.anneeScolaireId());

        Long classeId = request.classeId();
        Long anneeId = request.anneeScolaireId();
        Long ecoleId = classe.getEcole().getId();
        Long niveauId = classe.getNiveau().getId();
        String mois = request.mois();

        // Inscriptions valides de la classe/année : sert à vérifier que chaque
        // inscriptionId reçu appartient bien à cette classe et cette année.
        List<Inscription> inscriptionsValides =
                inscriptionRepository.findElevesValidesPourReleve(classeId, anneeId);

        Map<Long, Inscription> inscriptionsParId = inscriptionsValides.stream()
                .collect(Collectors.toMap(Inscription::getId, i -> i));

        // Coefficients du programme de cette classe (primaire, sans sous-groupe) :
        // sert à vérifier que chaque coefficientMatiereId reçu appartient bien
        // à cette école / cette année / cette classe (ou son niveau).
        Set<Long> coefficientsAutorises = coefficientMatiereRepository
                .findProgrammesPourClasse(ecoleId, anneeId, niveauId, classeId)
                .stream()
                .filter(cm -> cm.getSousGroupe() == null)
                .map(CoefficientMatiere::getId)
                .collect(Collectors.toSet());

        if (request.notes() == null) {
            return;
        }

        for (NotePrimaireRequest item : request.notes()) {

            Double valeur = item.note();

            if (valeur != null && (valeur < NOTE_MIN || valeur > NOTE_MAX)) {
                throw new IllegalArgumentException(
                        "La note primaire doit être comprise entre 0 et 10."
                );
            }

            Inscription inscription = inscriptionsParId.get(item.inscriptionId());
            if (inscription == null) {
                throw new IncompatibleClasseException(
                        "L'inscription " + item.inscriptionId()
                                + " n'appartient pas à cette classe pour cette année scolaire."
                );
            }

            if (!coefficientsAutorises.contains(item.coefficientMatiereId())) {
                throw new IncompatibleClasseException(
                        "La matière (coefficient " + item.coefficientMatiereId()
                                + ") ne fait pas partie du programme de cette classe/année."
                );
            }

            if (valeur == null) {
                // Valeur vide -> suppression de la note existante si présente.
                noteRepository.findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeIsNull(
                        item.inscriptionId(),
                        item.coefficientMatiereId(),
                        mois
                ).ifPresent(noteRepository::delete);
                continue;
            }

            CoefficientMatiere coefficient = coefficientMatiereRepository
                    .findById(item.coefficientMatiereId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ligne de programme introuvable : " + item.coefficientMatiereId()
                    ));

            Note note = noteRepository.findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeIsNull(
                    item.inscriptionId(),
                    item.coefficientMatiereId(),
                    mois
            ).orElse(new Note());

            note.setInscription(inscription);
            note.setEleve(inscription.getEleve());
            note.setClasse(inscription.getClasse());
            note.setAnneeScolaire(inscription.getAnneeScolaire());
            note.setMatiere(coefficient.getMatiere());
            note.setCoefficientMatiere(coefficient);

            note.setPeriode(mois);

            note.setNClass(valeur);
            note.setNExem(null);

            note.setCoeff(
                    coefficient.getCoefficient() != null
                            ? coefficient.getCoefficient()
                            : 1
            );

            note.setSousGroupe(null);

            noteRepository.save(note);
        }
    }

    // =========================================================
    // 🔧 UTILITAIRES PRIVÉS
    // =========================================================

    private Classe getClasse(Long classeId) {
        return classeRepository.findById(classeId)
                .orElseThrow(() -> new ResourceNotFoundException("Classe introuvable : " + classeId));
    }

    private void getAnneeScolaireOuThrow(Long anneeId) {
        anneeScolaireRepository.findById(anneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Année scolaire introuvable : " + anneeId));
    }

    private ElevePrimaireDto toEleveDto(Inscription inscription) {
        var eleve = inscription.getEleve();
        return new ElevePrimaireDto(
                inscription.getId(),
                eleve.getId(),
                eleve.getNom(),
                eleve.getPrenom(),
                eleve.getMatricule()
        );
    }

    /**
     * NOTE IMPORTANTE :
     * Le modèle actuel (Matiere / CoefficientMatiere) ne possède PAS de colonne
     * "ordreAffichage". Pour respecter le format de DTO demandé sans inventer
     * une colonne en base, "ordreAffichage" est ici calculé comme la position
     * de la matière dans la liste déjà triée (nom ASC, puis id ASC), à usage
     * d'affichage frontend uniquement — ce n'est pas une donnée persistée.
     */
    private List<MatierePrimaireDto> toMatiereDtos(List<CoefficientMatiere> programme) {
        List<MatierePrimaireDto> resultat = new java.util.ArrayList<>();
        int position = 0;
        for (CoefficientMatiere cm : programme) {
            resultat.add(new MatierePrimaireDto(
                    cm.getId(),
                    cm.getMatiere().getId(),
                    cm.getMatiere().getNom(),
                    cm.getCoefficient() != null ? cm.getCoefficient().doubleValue() : null,
                    position++
            ));
        }
        return resultat;
    }

    private NotePrimaireDto toNoteDto(Note note) {
        return new NotePrimaireDto(
                note.getId(),
                note.getInscription().getId(),
                note.getCoefficientMatiere().getId(),
                note.getNClass()
        );
    }
}