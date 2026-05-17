package com.saas.school.service;

import com.saas.school.dto.NoteReponseDTO;
import com.saas.school.dto.NoteRequestDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteRepository noteRepository;
    private final EleveRepository eleveRepository;
    private final MatiereRepository matiereRepository;
    private final ClasseRepository classeRepository;
    private final AnneeScolaireRepository anneeRepository;
    private final InscriptionRepository inscriptionRepository;
    private final EcoleRepository ecoleRepository;
    private final AbonnementService abonnementService;

    public List<NoteReponseDTO> getNotes(Long classeId, Long anneeScolaireId, Long eleveId, String periode) {

        if (classeId == null || anneeScolaireId == null) {
            throw new RuntimeException("classeId et anneeId sont obligatoires");
        }

        // 🔥 Vérification eleve seulement si fourni
        if (eleveId != null) {
            eleveRepository.findById(eleveId)
                    .orElseThrow(() -> new RuntimeException("Eleve introuvable"));
        }

        List<Note> notes;

        // 🔥 Cas 1 : eleve + periode
        if (eleveId != null && periode != null) {
            notes = noteRepository.findByClasseIdAndAnneeScolaireIdAndEleveIdAndPeriode(
                    classeId, anneeScolaireId, eleveId, periode
            );

            // 🔥 Cas 2 : eleve seulement
        } else if (eleveId != null) {
            notes = noteRepository.findByClasseIdAndAnneeScolaireIdAndEleveId(
                    classeId, anneeScolaireId, eleveId
            );

            // 🔥 Cas 3 : periode seulement
        } else if (periode != null) {
            notes = noteRepository.findByClasseIdAndAnneeScolaireIdAndPeriode(
                    classeId, anneeScolaireId, periode
            );

            // 🔥 Cas 4 : tout
        } else {
            notes = noteRepository.findByClasseIdAndAnneeScolaireId(
                    classeId, anneeScolaireId
            );
        }

        // 🔥 Mapping vers DTO
        return notes.stream()
                .map(this::mapToDTO)
                .toList();
    }
    private NoteReponseDTO mapToDTO(Note note) {

        double nClass = note.getNClass() != null ? note.getNClass() : 0.0;
        double nExem = note.getNExem() != null ? note.getNExem() : 0.0;

        double moyenne = (nClass + nExem) / 2 * (note.getCoeff() != null ? note.getCoeff() : 1.0);

        NoteReponseDTO dto = new NoteReponseDTO();
        dto.setId(note.getId());
        dto.setEleveNom(note.getEleve().getNom());
        dto.setMatiereNom(note.getMatiere().getNom());
        dto.setClasseNom(note.getClasse().getNomComplet());
        dto.setAnnee(note.getAnneeScolaire().getNom());
        dto.setPeriode(note.getPeriode());
        dto.setNClass(note.getNClass());
        dto.setNExem(note.getNExem());
        dto.setCoeff(note.getCoeff());
        dto.setMoyenne(moyenne);

        return dto;
    }
    public NoteReponseDTO ajouter(NoteRequestDTO dto) {

        Eleve eleve = eleveRepository.findById(dto.getEleveId())
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        Matiere matiere = matiereRepository.findById(dto.getMatiereId())
                .orElseThrow(() -> new RuntimeException("Matière introuvable"));

        Classe classe = classeRepository.findById(dto.getClasseId())
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        AnneeScolaire annee = anneeRepository.findById(dto.getAnneeScolaireId())
                .orElseThrow(() -> new RuntimeException("Année introuvable"));

        // 🔥 cohérence inscription


        // validation
        if (dto.getNClass() == null || dto.getNExem() == null) {
            throw new RuntimeException("Les notes sont obligatoires");
        }

        if (dto.getCoeff() == null) {
            dto.setCoeff(1.0);
        }

        boolean exists = noteRepository
                .existsByEleveAndMatiereAndClasseAndAnneeScolaireAndPeriode(
                        eleve, matiere, classe, annee, dto.getPeriode()
                );

        if (exists) {
            throw new RuntimeException("Note déjà existante !");
        }

        Note note = new Note();
        note.setEleve(eleve);
        note.setMatiere(matiere);
        note.setClasse(classe);
        note.setAnneeScolaire(annee);
        note.setPeriode(dto.getPeriode());
        note.setNClass(dto.getNClass());
        note.setNExem(dto.getNExem());
        note.setCoeff(dto.getCoeff());
        Ecole ecole = ecoleRepository.findById(note.getClasse().getEcole().getId())
                .orElseThrow(() -> new RuntimeException("École introuvable"));
        if (!abonnementService.isActif(ecole)) {
            throw new RuntimeException("🚫 Abonnement expiré");
        }

        note = noteRepository.save(note);

        double nClass = note.getNClass() != null ? note.getNClass() : 0.0;
        double nExem = note.getNExem() != null ? note.getNExem() : 0.0;

        double moyenne = ((nClass + nExem) / 2) * note.getCoeff();

        NoteReponseDTO res = new NoteReponseDTO();
        res.setId(note.getId());
        res.setEleveNom(eleve.getNom());
        res.setMatiereNom(matiere.getNom());
        res.setClasseNom(classe.getNomComplet());
        res.setAnnee(annee.getNom());
        res.setPeriode(note.getPeriode());
        res.setNClass(note.getNClass());
        res.setNExem(note.getNExem());
        res.setCoeff(note.getCoeff());
        res.setMoyenne(moyenne);

        return res;
    }
    public Double calculMoyenneEleve(List<Note> notes) {

        double total = 0;
        double coeff = 0;

        for (Note n : notes) {

            double nClass = n.getNClass() != null ? n.getNClass() : 0.0;
            double nExem = n.getNExem() != null ? n.getNExem() : 0.0;

            double moy = (nClass + nExem) / 2;

            double c = n.getCoeff() != null ? n.getCoeff() : 1.0;

            total += moy * c;
            coeff += c;
        }

        return coeff == 0 ? 0 : total / coeff;
    }
}