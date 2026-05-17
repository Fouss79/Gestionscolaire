package com.saas.school.service;

import com.saas.school.dto.EmploiDto;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EmploiDuTempsService {

    private final ClasseRepository classeRepo;
    private final HabilitationRepository habilitationRepo;
    private final BesoinHeureRepository besoinRepo;
    private final EmploiDuTempsRepository edtRepo;
    private final MatiereClasseRepository mcRepos;
    private final AffectationRepository affectationRepository;


    private final String[] jours = {"LUNDI", "MARDI", "MERCREDI", "JEUDI", "VENDREDI"};

    private final int HEURE_DEBUT = 8;
    private final int HEURE_FIN = 18;
    private final int SAUT_JOUR = 2;


    @Transactional
    public EmploiDuTemps create(EmploiDto dto) {

        Classe classe = classeRepo.findById(dto.getClasseId())
                .orElseThrow(() -> new RuntimeException("Classe introuvable"));

        MatiereClasse mc = mcRepos
                .findByMatiereIdAndClasseIdAndAnneeScolaireId(
                        dto.getMatiereId(),
                        dto.getClasseId(),
                        dto.getAnneeId()
                )
                .orElseThrow(() -> new RuntimeException("Matière non assignée"));

        int dejaUtilise = edtRepo.totalHeuresDejaPlanifiees(
                dto.getClasseId(),
                dto.getMatiereId(),
                dto.getAnneeId()
        );

        int restant = mc.getNombreHeures() - dejaUtilise;

        if (restant <= 0) {
            throw new RuntimeException("Toutes les heures sont déjà planifiées");
        }

        int duree = dto.getHeureFin() - dto.getHeureDebut();

        if (duree > restant) {
            throw new RuntimeException(
                    "Heures insuffisantes. Restant: " + restant
            );
        }

        // conflit prof
        boolean conflitProf = edtRepo.existsByEnseignantIdAndJourAndHeureDebutLessThanAndHeureFinGreaterThan(
                dto.getEnseignantId(),
                dto.getJour(),
                dto.getHeureFin(),
                dto.getHeureDebut()
        );

        if (conflitProf) {
            throw new RuntimeException("Conflit enseignant");
        }

        // conflit classe
        boolean conflitClasse = edtRepo.existsByClasseIdAndJourAndHeureDebutLessThanAndHeureFinGreaterThan(
                dto.getClasseId(),
                dto.getJour(),
                dto.getHeureFin(),
                dto.getHeureDebut()
        );

        if (conflitClasse) {
            throw new RuntimeException("Conflit classe");
        }

        EmploiDuTemps edt = new EmploiDuTemps();
        edt.setClasse(classe);

        Matiere matiere = new Matiere();
        matiere.setId(dto.getMatiereId());

        Enseignant enseignant = new Enseignant();
        enseignant.setId(dto.getEnseignantId());

        AnneeScolaire annee = new AnneeScolaire();
        annee.setId(dto.getAnneeId());

        edt.setMatiere(matiere);
        edt.setEnseignant(enseignant);
        edt.setAnneeScolaire(annee);

        edt.setJour(dto.getJour());
        edt.setHeureDebut(dto.getHeureDebut());
        edt.setHeureFin(dto.getHeureFin());

        return edtRepo.save(edt);
    }


    public List<EmploiDuTemps> getByJourEtClasse(String jour, Long classeId) {
        return edtRepo.findByJourAndClasseId(jour, classeId);
    }
    public List<EmploiDuTemps> filtrer(Long classeId, Long matiereId, String jour) {
        return edtRepo.findByClasseIdAndMatiereIdAndJour(classeId, matiereId, jour);
    }
    public List<EmploiDuTemps> filtre(Long classeId, Long anneeId, String jour) {
        return edtRepo.filtre(classeId, anneeId, jour);
    }

    @Transactional
    public void generer(Long anneeId) {

        // 🔥 nettoyage
        edtRepo.deleteByAnneeScolaireId(anneeId);

        List<Classe> classes = classeRepo.findAll();

        for (Classe classe : classes) {

            List<MatiereClasse> besoins =
                    mcRepos.findByClasseIdAndAnneeScolaireId(classe.getId(), anneeId);

            if (besoins.isEmpty()) continue;

            // 🔥 pool de matières actives
            List<MatiereClasse> pool = new ArrayList<>(besoins);

            Map<Long, Integer> restant = new HashMap<>();
            for (MatiereClasse b : pool) {
                restant.put(b.getId(), b.getNombreHeures());
            }

            int jourIndex = 0;
            int heureCourante = HEURE_DEBUT;

            int securiteGlobale = 0;

            while (!pool.isEmpty()) {

                securiteGlobale++;
                if (securiteGlobale > 50000) {
                    System.out.println("❌ Sécurité boucle globale déclenchée");
                    break;
                }

                Iterator<MatiereClasse> it = pool.iterator();

                while (it.hasNext()) {

                    MatiereClasse b = it.next();

                    int heuresRestantes = restant.getOrDefault(b.getId(), 0);

                    if (heuresRestantes <= 0) {
                        it.remove();
                        continue;
                    }

                    // 🔥 habilitation
                    Affectation a = affectationRepository
                            .findByClasseIdAndMatiereIdAndAnneeScolaireId(
                                    classe.getId(),
                                    b.getMatiere().getId(),
                                    anneeId
                            ).orElse(null);

                    if (a == null) {
                        System.out.println("❌ Pas d'affectation");
                        continue;
                    }
                    // 🔥 petit bloc pour alternance
                    int duree = Math.min(2, heuresRestantes);

                    // 🔥 changement jour si dépasse
                    if (heureCourante + duree > HEURE_FIN) {
                        heureCourante = HEURE_DEBUT;
                        jourIndex++;

                        if (jourIndex >= jours.length) {
                            System.out.println("⚠️ semaine saturée pour " + classe.getNomComplet());
                            return;
                        }
                        continue;
                    }

                    String jour = jours[jourIndex];

                    // 🔥 conflit enseignant
                    boolean conflitProf = edtRepo.existsByEnseignantIdAndJourAndHeureDebutLessThanAndHeureFinGreaterThan(
                            a.getEnseignant().getId(),
                            jour,
                            heureCourante + duree,
                            heureCourante
                    );

                    if (conflitProf) {
                        continue;
                    }

                    // 🔥 conflit classe
                    boolean conflitClasse = edtRepo.existsByClasseIdAndJourAndHeureDebutLessThanAndHeureFinGreaterThan(
                            classe.getId(),
                            jour,
                            heureCourante + duree,
                            heureCourante
                    );

                    if (conflitClasse) {
                        continue;
                    }

                    // 🔥 création EDT
                    EmploiDuTemps edt = new EmploiDuTemps();
                    edt.setClasse(classe);
                    edt.setMatiere(b.getMatiere());
                    edt.setEnseignant(a.getEnseignant());
                    edt.setAnneeScolaire(b.getAnneeScolaire());

                    edt.setJour(jour);
                    edt.setHeureDebut(heureCourante);
                    edt.setHeureFin(heureCourante + duree);

                    edtRepo.save(edt);

                    // 🔥 update état
                    heureCourante += duree;
                    restant.put(b.getId(), heuresRestantes - duree);

                    if (restant.get(b.getId()) <= 0) {
                        it.remove();
                    }

                    // 🔥 reset journée
                    if (heureCourante >= HEURE_FIN) {
                        heureCourante = HEURE_DEBUT;
                        jourIndex++;

                        if (jourIndex >= jours.length) {
                            System.out.println("⚠️ semaine terminée pour " + classe.getNomComplet());
                            return;
                        }
                    }
                }
            }
        }
    }


    private boolean conflitHoraireMatiere(
            Classe classe,
            MatiereClasse b,
            Long anneeId,
            int debut,
            int fin
    ) {

        List<EmploiDuTemps> existants = edtRepo
                .findByClasseIdAndMatiereIdAndAnneeScolaireId(
                        classe.getId(),
                        b.getMatiere().getId(),
                        anneeId
                );

        for (EmploiDuTemps e : existants) {

            // 🔥 chevauchement horaire (peu importe le jour)
            if (e.getHeureDebut() < fin && e.getHeureFin() > debut) {
                return true;
            }
        }

        return false;
    }
    public List<EmploiDuTemps> getByClasse(Long classeId, Long anneeId) {
        return edtRepo.findByClasseIdAndAnneeScolaireId(classeId, anneeId);
    }
    private boolean respectIntervalJour(
            Classe classe,
            MatiereClasse b,
            Long anneeId,
            int jourIndex
    ) {

        List<EmploiDuTemps> existants = edtRepo
                .findByClasseIdAndMatiereIdAndAnneeScolaireId(
                        classe.getId(),
                        b.getMatiere().getId(),
                        anneeId
                );

        for (EmploiDuTemps e : existants) {

            int indexExistant = getJourIndex(e.getJour());

            // 🔥 distance minimale = 2 jours
            if (Math.abs(indexExistant - jourIndex) < 2) {
                return false;
            }
        }

        return true;
    }
    private int getJourIndex(String jour) {
        for (int i = 0; i < jours.length; i++) {
            if (jours[i].equals(jour)) {
                return i;
            }
        }
        return -1;
    }

}