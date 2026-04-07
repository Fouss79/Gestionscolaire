package com.saas.school.service;

import com.saas.school.dto.MatiereclasseDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatiereClasseService {

    private final MatiereClasseRepository repository;
    private final MatiereRepository matiereRepository;
    private final ClasseRepository classeRepository;
    private final AnneeScolaireRepository anneeRepository;
    private final EcoleRepository ecoleRepository;

    public MatiereClasse create(MatiereclasseDTO mc) {

        System.out.println("matiereId = " + mc.getMatiereId());
        System.out.println("classeId = " + mc.getClasseId());
        System.out.println("anneeId = " + mc.getAnneeScolaireId());

        // 🔥 Vérification des IDs obligatoires
        if (mc.getMatiereId() == null || mc.getClasseId() == null) {
            throw new RuntimeException("Matière et Classe sont obligatoires");
        }

        Matiere matiere = matiereRepository.findById(mc.getMatiereId())
                .orElseThrow(() -> new RuntimeException("Matière introuvable"));

        Classe classe = classeRepository.findById(mc.getClasseId())
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        // 🔥 Gestion de l'année (peut être null)
        AnneeScolaire annee = null;
        if (mc.getAnneeScolaireId() != null) {
            annee = anneeRepository.findById(mc.getAnneeScolaireId())
                    .orElseThrow(() -> new RuntimeException("Année introuvable"));
        }

        Ecole ecole = classe.getEcole();

        if (ecole == null) {
            throw new RuntimeException("Classe sans école !");
        }

        // 🔥 Vérification doublon (seulement si année existe)
        boolean existe;
        if (annee != null) {
            existe = repository.existsByMatiereIdAndClasseIdAndAnneeScolaireIdAndEcoleId(
                    mc.getMatiereId(),
                    mc.getClasseId(),
                    mc.getAnneeScolaireId(),
                    ecole.getId()
            );
        } else {
            existe = repository.existsByMatiereIdAndClasseIdAndEcoleId(
                    mc.getMatiereId(),
                    mc.getClasseId(),
                    ecole.getId()
            );
        }

        if (existe) {
            throw new RuntimeException("Cette matière existe déjà !");
        }

        MatiereClasse m = new MatiereClasse();
        m.setMatiere(matiere);
        m.setClasse(classe);
        m.setAnneeScolaire(annee); // peut être null
        m.setEcole(ecole);
        m.setCoefficient(mc.getCoefficient());
        m.setNombreHeures(mc.getNombreHeures());

        return repository.save(m);
    }

    public List<MatiereClasse> getByClasseAndAnnee(Long classeId, Long anneeId) {

        if (classeId == null) {
            throw new RuntimeException("classeId est obligatoire");
        }

        // 🔥 si année fournie
        if (anneeId != null) {
            return repository.findByClasseIdAndAnneeScolaireId(classeId, anneeId);
        }

        // 🔥 sinon sans année
        return repository.findByClasseId(classeId);
    }

    public int getCoefficient(Long matiereId, Long classeId, Long anneeId) {
        return repository.findByMatiereIdAndClasseIdAndAnneeScolaireId(
                        matiereId, classeId, anneeId
                )
                .map(MatiereClasse::getCoefficient)
                .orElse(1);
    }
}