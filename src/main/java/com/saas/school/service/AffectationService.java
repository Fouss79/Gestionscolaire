package com.saas.school.service;

import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AffectationService {

    private final AffectationRepository affectationRepo;
    private final HabilitationRepository habilitationRepo;
    private final ClasseRepository classeRepository;
    private  final EnseignantRepository enseignantRepository;
    private  final AnneeScolaireRepository anneeScolaireRepository;
    private final MatiereRepository matiereRepository;
    public Affectation create(Long enseignantId, Long classeId, Long matiereId, Long anneeId) {

        // 🔥 vérifier habilitation
        boolean habilite = habilitationRepo
                .existsByEnseignantIdAndMatiereIdAndAnneeScolaireId(
                        enseignantId,
                        matiereId,
                        anneeId
                );

        if (!habilite) {
            throw new RuntimeException("❌ Enseignant non habilité");
        }

        // 🔥 éviter doublon
        boolean existe = affectationRepo
                .existsByClasseIdAndMatiereIdAndAnneeScolaireId(
                        classeId,
                        matiereId,
                        anneeId
                );
Enseignant e = enseignantRepository.findById(enseignantId).orElseThrow(()-> new RuntimeException("Enseignant introuvable"));
    Classe c = classeRepository.findById(classeId).orElseThrow(() ->new RuntimeException("Classe introuvable"));  
 Matiere m = matiereRepository.findById(matiereId).orElseThrow(() -> new RuntimeException("Matiere introuvable"));
 AnneeScolaire annee = anneeScolaireRepository.findById(anneeId).orElseThrow(() -> new RuntimeException("Annee scolaire introuvable"));
        if (existe) {
            throw new RuntimeException("❌ Déjà affecté");
        }

        Affectation a = new Affectation();


        a.setEnseignant(e);
        a.setClasse(c);
        a.setMatiere(m);
        a.setAnneeScolaire(annee);

        return affectationRepo.save(a);
    }

    public List<Affectation> getByClasse(Long classeId, Long anneeId) {
        return affectationRepo.findByClasseIdAndAnneeScolaireId(classeId, anneeId);
    }
}
