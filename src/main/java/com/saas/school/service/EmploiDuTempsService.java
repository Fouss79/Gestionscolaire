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
    private final AffectationEnseignantRepository affectationRepo;
    private final EmploiDuTempsRepository edtRepo;
    private final SalleRepository salleRepo;
    private final EmargementRepository emargementRepo;

    // 🔥 AJOUTS
    private final SousGroupeRepository sousGroupeRepo;
    private final CoefficientMatiereRepository coefficientRepo;

    private final String[] jours = {
            "LUNDI",
            "MARDI",
            "MERCREDI",
            "JEUDI",
            "VENDREDI"
    };

    private static final int HEURE_DEBUT = 8;
    private static final int HEURE_FIN = 18;
    private static final int SECURITE_MAX_ITERATIONS = 50000;


    // =========================================================
    // 🟢 RÉCUPÉRER LE SOUS-GROUPE
    // =========================================================

    private SousGroupe getSousGroupe(Long sousGroupeId) {

        if (sousGroupeId == null) {
            return null;
        }

        return sousGroupeRepo.findById(sousGroupeId)
                .orElseThrow(() ->
                        new RuntimeException("Sous-groupe introuvable"));
    }


    // =========================================================
    // 🔥 DÉTERMINER LE PROGRAMME APPLICABLE
    // =========================================================
    /*
     * Priorité :
     *
     * 1. Coefficient spécifique au sous-groupe
     * 2. Coefficient du programme général
     *
     * Exemple :
     *
     * Programme Math = 4h
     *
     * Sous-groupe A = 2h
     * Sous-groupe B = 3h
     *
     * Si on crée pour A → 2h
     * Si on crée pour B → 3h
     * Sans sous-groupe → 4h
     */
    private CoefficientMatiere obtenirProgrammeApplicable(
            AffectationEnseignant affectation,
            Long sousGroupeId
    ) {

        CoefficientMatiere programmeGeneral =
                affectation.getCoefficientMatiere();

        if (sousGroupeId == null) {
            return programmeGeneral;
        }

        /*
         * On cherche d'abord le coefficient spécifique
         * au sous-groupe.
         */
        Optional<CoefficientMatiere> coefficientSousGroupe =
                coefficientRepo
                        .findByMatiereIdAndNiveauIdAndAnneeScolaireIdAndClasseIdAndSousGroupeId(
                                programmeGeneral.getMatiere().getId(),
                                programmeGeneral.getNiveau().getId(),
                                programmeGeneral.getAnneeScolaire().getId(),
                                programmeGeneral.getClasse().getId(),
                                sousGroupeId
                        );

        /*
         * S'il existe, il est prioritaire.
         *
         * Sinon on utilise le programme général.
         */
        return coefficientSousGroupe.orElse(programmeGeneral);
    }


    // =========================================================
    // 🔥 CALCUL DES HEURES DÉJÀ PLANIFIÉES
    // =========================================================

    private int heuresDejaPlanifiees(
            Long classeId,
            Long matiereId,
            Long anneeId,
            Long sousGroupeId
    ) {

        /*
         * Cours spécifique à un sous-groupe
         */
        if (sousGroupeId != null) {

            return edtRepo.totalHeuresDejaPlanifieesSousGroupe(
                    classeId,
                    matiereId,
                    anneeId,
                    sousGroupeId
            );
        }

        /*
         * Cours général de la classe
         */
        return edtRepo.totalHeuresDejaPlanifiees(
                classeId,
                matiereId,
                anneeId
        );
    }


    // =========================================================
    // 🟡 CRÉATION MANUELLE
    // =========================================================

    @Transactional
    public EmploiDuTemps create(EmploiDto dto) {

        return creerCreneauInterne(dto, null);
    }


    // =========================================================
    // 🟡 MODIFICATION
    // =========================================================

    @Transactional
    public EmploiDuTemps update(Long id, EmploiDto dto) {

        EmploiDuTemps edt = edtRepo.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Créneau introuvable"));

        Classe classe = classeRepo.findById(dto.getClasseId())
                .orElseThrow(() ->
                        new RuntimeException("Classe introuvable"));

        AffectationEnseignant affectation =
                affectationRepo
                        .findByEnseignantIdAndClasseIdAndCoefficientMatiere_MatiereIdAndCoefficientMatiere_AnneeScolaireId(
                                dto.getEnseignantId(),
                                dto.getClasseId(),
                                dto.getMatiereId(),
                                dto.getAnneeId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cet enseignant n'est pas affecté à cette matière dans cette classe pour cette année"
                                ));

        // 🔥 Sous-groupe
        SousGroupe sousGroupe =
                getSousGroupe(dto.getSousGroupeId());

        // 🔥 Programme applicable
        CoefficientMatiere programme =
                obtenirProgrammeApplicable(
                        affectation,
                        dto.getSousGroupeId()
                );


        // =====================================================
        // 🔥 CALCUL DES HEURES
        // =====================================================

        int ancienneDuree =
                edt.getHeureFin() - edt.getHeureDebut();

        int dejaUtilise =
                heuresDejaPlanifiees(
                        dto.getClasseId(),
                        dto.getMatiereId(),
                        dto.getAnneeId(),
                        dto.getSousGroupeId()
                );

        // On retire l'ancien créneau
        dejaUtilise -= ancienneDuree;

        if (dejaUtilise < 0) {
            dejaUtilise = 0;
        }

        int quotaHebdo =
                programme.getNombreHeuresParSemaine() != null
                        ? programme.getNombreHeuresParSemaine()
                        : 0;

        int restant =
                quotaHebdo - dejaUtilise;


        // =====================================================
        // ⏱️ NOUVELLE DURÉE
        // =====================================================

        int nouvelleDuree =
                dto.getHeureFin() - dto.getHeureDebut();

        if (nouvelleDuree <= 0) {

            throw new RuntimeException(
                    "La durée du créneau doit être supérieure à 0"
            );
        }

        if (nouvelleDuree > restant) {

            throw new RuntimeException(
                    "Heures insuffisantes pour "
                            + (sousGroupe != null
                            ? "le sous-groupe"
                            : "la classe")
                            + ". Restant : "
                            + restant
                            + "h"
            );
        }


        // =====================================================
        // 🏫 SALLE
        // =====================================================

        Salle salle = null;

        if (dto.getSalleId() != null) {

            salle = salleRepo.findById(dto.getSalleId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Salle introuvable"
                            ));

        } else if (classe.getSalle() != null) {

            salle = classe.getSalle();
        }


        // =====================================================
        // 🚫 CONFLITS
        // =====================================================

        verifierConflits(
                id,
                dto.getEnseignantId(),
                dto.getClasseId(),
                dto.getSousGroupeId(),
                salle != null ? salle.getId() : null,
                dto.getAnneeId(),
                dto.getJour(),
                dto.getHeureDebut(),
                dto.getHeureFin()
        );


        // =====================================================
        // ✏️ MODIFICATION
        // =====================================================

        edt.setClasse(classe);
        edt.setMatiere(programme.getMatiere());
        edt.setEnseignant(affectation.getEnseignant());
        edt.setAnneeScolaire(programme.getAnneeScolaire());
        edt.setSalle(salle);
        edt.setSousGroupe(sousGroupe);

        edt.setJour(dto.getJour());
        edt.setHeureDebut(dto.getHeureDebut());
        edt.setHeureFin(dto.getHeureFin());

        return edtRepo.save(edt);
    }


    // =========================================================
    // 🔥 LOGIQUE COMMUNE CREATE / UPDATE
    // =========================================================

    private EmploiDuTemps creerCreneauInterne(
            EmploiDto dto,
            Long idEnCoursDeModification
    ) {

        Classe classe = classeRepo.findById(dto.getClasseId())
                .orElseThrow(() ->
                        new RuntimeException("Classe introuvable"));


        AffectationEnseignant affectation =
                affectationRepo
                        .findByEnseignantIdAndClasseIdAndCoefficientMatiere_MatiereIdAndCoefficientMatiere_AnneeScolaireId(
                                dto.getEnseignantId(),
                                dto.getClasseId(),
                                dto.getMatiereId(),
                                dto.getAnneeId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Cet enseignant n'est pas affecté à cette matière "
                                                + "dans cette classe pour cette année"
                                ));


        // =====================================================
        // 🔥 SOUS-GROUPE
        // =====================================================

        SousGroupe sousGroupe =
                getSousGroupe(dto.getSousGroupeId());


        // =====================================================
        // 🔥 PROGRAMME APPLICABLE
        // =====================================================

        CoefficientMatiere programme =
                obtenirProgrammeApplicable(
                        affectation,
                        dto.getSousGroupeId()
                );


        // =====================================================
        // 🔥 HEURES DÉJÀ UTILISÉES
        // =====================================================

        int dejaUtilise =
                heuresDejaPlanifiees(
                        dto.getClasseId(),
                        dto.getMatiereId(),
                        dto.getAnneeId(),
                        dto.getSousGroupeId()
                );


        // =====================================================
        // 🔥 MODIFICATION
        // =====================================================

        if (idEnCoursDeModification != null) {

            EmploiDuTemps ancien =
                    edtRepo.findById(idEnCoursDeModification)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Créneau à modifier introuvable"
                                    ));

            int ancienneDuree =
                    ancien.getHeureFin()
                            - ancien.getHeureDebut();

            dejaUtilise -= ancienneDuree;

            if (dejaUtilise < 0) {
                dejaUtilise = 0;
            }
        }


        // =====================================================
        // 🔥 QUOTA
        // =====================================================

        int quotaHebdo =
                programme.getNombreHeuresParSemaine() != null
                        ? programme.getNombreHeuresParSemaine()
                        : 0;

        int restant =
                quotaHebdo - dejaUtilise;


        if (restant <= 0) {

            throw new RuntimeException(
                    "Toutes les heures prévues pour "
                            + (sousGroupe != null
                            ? "ce sous-groupe"
                            : "cette matière")
                            + " sont déjà planifiées"
            );
        }


        // =====================================================
        // ⏱️ DURÉE
        // =====================================================

        int duree =
                dto.getHeureFin()
                        - dto.getHeureDebut();

        if (duree <= 0) {

            throw new RuntimeException(
                    "L'heure de fin doit être supérieure "
                            + "à l'heure de début"
            );
        }

        if (duree > restant) {

            throw new RuntimeException(
                    "Heures insuffisantes. Restant : "
                            + restant
                            + "h"
            );
        }


        // =====================================================
        // 🏫 SALLE
        // =====================================================

        Salle salle = null;

        if (dto.getSalleId() != null) {

            salle = salleRepo.findById(dto.getSalleId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Salle introuvable"
                            ));

        } else if (classe.getSalle() != null) {

            salle = classe.getSalle();
        }


        // =====================================================
        // 🚫 CONFLITS
        // =====================================================

        verifierConflits(
                idEnCoursDeModification,
                dto.getEnseignantId(),
                dto.getClasseId(),
                dto.getSousGroupeId(),
                salle != null ? salle.getId() : null,
                dto.getAnneeId(),
                dto.getJour(),
                dto.getHeureDebut(),
                dto.getHeureFin()
        );


        // =====================================================
        // 🔥 CRÉATION / MODIFICATION
        // =====================================================

        EmploiDuTemps edt;

        if (idEnCoursDeModification != null) {

            edt = edtRepo.findById(idEnCoursDeModification)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Créneau à modifier introuvable"
                            ));

        } else {

            edt = new EmploiDuTemps();
        }


        // =====================================================
        // ✏️ DONNÉES
        // =====================================================

        edt.setClasse(classe);
        edt.setMatiere(programme.getMatiere());
        edt.setEnseignant(affectation.getEnseignant());
        edt.setAnneeScolaire(programme.getAnneeScolaire());
        edt.setSalle(salle);

        // 🔥 IMPORTANT
        edt.setSousGroupe(sousGroupe);

        edt.setJour(dto.getJour());
        edt.setHeureDebut(dto.getHeureDebut());
        edt.setHeureFin(dto.getHeureFin());


        return edtRepo.save(edt);
    }


    // =========================================================
    // 🚫 CONFLITS
    // =========================================================

    private void verifierConflits(
            Long emploiDuTempsId,
            Long enseignantId,
            Long classeId,
            Long sousGroupeId,
            Long salleId,
            Long anneeId,
            String jour,
            int debut,
            int fin
    ) {

        // =====================================================
        // 👨‍🏫 PROFESSEUR
        // =====================================================

        boolean conflitProf =
                edtRepo.existsConflitEnseignant(
                        emploiDuTempsId,
                        enseignantId,
                        anneeId,
                        jour,
                        fin,
                        debut
                );

        if (conflitProf) {

            throw new RuntimeException(
                    "Conflit : cet enseignant a déjà "
                            + "un cours sur ce créneau"
            );
        }


        // =====================================================
        // 👨‍🎓 CLASSE + SOUS-GROUPE
        // =====================================================

        boolean conflitClasse =
                edtRepo.existsConflitClasseAvecSousGroupe(
                        emploiDuTempsId,
                        classeId,
                        sousGroupeId,
                        anneeId,
                        jour,
                        fin,
                        debut
                );

        if (conflitClasse) {

            throw new RuntimeException(
                    "Conflit : ce groupe d'élèves "
                            + "a déjà un cours sur ce créneau"
            );
        }


        // =====================================================
        // 🏫 SALLE
        // =====================================================

        if (salleId != null) {

            boolean conflitSalle =
                    edtRepo.existsConflitSalle(
                            emploiDuTempsId,
                            salleId,
                            anneeId,
                            jour,
                            fin,
                            debut
                    );

            if (conflitSalle) {

                throw new RuntimeException(
                        "Conflit : cette salle est déjà "
                                + "occupée sur ce créneau"
                );
            }
        }
    }


    // =========================================================
    // 📥 CONSULTATION
    // =========================================================

    public List<EmploiDuTemps> getByJourEtClasse(
            String jour,
            Long classeId
    ) {

        return edtRepo.findByJourAndClasseId(
                jour,
                classeId
        );
    }


    public List<EmploiDuTemps> filtrer(
            Long classeId,
            Long matiereId,
            String jour
    ) {

        return edtRepo.findByClasseIdAndMatiereIdAndJour(
                classeId,
                matiereId,
                jour
        );
    }


    public List<EmploiDuTemps> filtre(
            Long classeId,
            Long anneeId,
            String jour
    ) {

        return edtRepo.filtre(
                classeId,
                anneeId,
                jour
        );
    }


    public List<EmploiDuTemps> getByClasse(
            Long classeId,
            Long anneeId
    ) {

        return edtRepo.findByClasseIdAndAnneeScolaireId(
                classeId,
                anneeId
        );
    }


    // =========================================================
    // 🗑️ SUPPRESSION
    // =========================================================

    @Transactional
    public void supprimer(Long id) {

        EmploiDuTemps edt =
                edtRepo.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Créneau introuvable"
                                ));

        if (emargementRepo.existsByEmploiDuTempsId(id)) {

            throw new RuntimeException(
                    "Impossible de supprimer ce créneau : "
                            + "il possède déjà des émargements."
            );
        }

        edtRepo.delete(edt);
    }


    // =========================================================
    // 🔁 GÉNÉRATION AUTOMATIQUE
    // =========================================================

    @Transactional
    public void generer(Long anneeId) {

        edtRepo.deleteByAnneeScolaireId(anneeId);

        List<Classe> classes =
                classeRepo.findAll();


        for (Classe classe : classes) {

            List<AffectationEnseignant> affectations =
                    affectationRepo
                            .findByClasseIdAndCoefficientMatiere_AnneeScolaireId(
                                    classe.getId(),
                                    anneeId
                            );

            if (affectations.isEmpty()) {
                continue;
            }


            // =================================================
            // 🔥 POOL
            // =================================================

            List<AffectationEnseignant> pool =
                    new ArrayList<>(affectations);


            pool.sort((a, b) ->
                    Integer.compare(
                            b.getCoefficientMatiere()
                                    .getCoefficient(),

                            a.getCoefficientMatiere()
                                    .getCoefficient()
                    )
            );


            // =================================================
            // 🔥 HEURES RESTANTES
            // =================================================

            Map<Long, Integer> restant =
                    new HashMap<>();


            for (AffectationEnseignant a : pool) {

                CoefficientMatiere programme =
                        a.getCoefficientMatiere();

                Integer heures =
                        programme
                                .getNombreHeuresParSemaine();


                restant.put(
                        a.getId(),
                        heures != null
                                ? heures
                                : 0
                );
            }


            Salle salleClasse =
                    classe.getSalle();


            int jourIndex = 0;

            int heureCourante =
                    HEURE_DEBUT;

            int securite = 0;

            boolean semaineSaturee =
                    false;


            // =================================================
            // 🔥 GÉNÉRATION
            // =================================================

            while (
                    !pool.isEmpty()
                            && !semaineSaturee
            ) {

                securite++;

                if (securite >
                        SECURITE_MAX_ITERATIONS) {

                    System.out.println(
                            "❌ Sécurité boucle globale déclenchée pour "
                                    + classe.getNomComplet()
                    );

                    break;
                }


                Iterator<AffectationEnseignant> it =
                        pool.iterator();


                while (it.hasNext()) {

                    AffectationEnseignant a =
                            it.next();


                    CoefficientMatiere programme =
                            a.getCoefficientMatiere();


                    int heuresRestantes =
                            restant.getOrDefault(
                                    a.getId(),
                                    0
                            );


                    if (heuresRestantes <= 0) {

                        it.remove();

                        continue;
                    }


                    // =============================================
                    // ⏱️ CRÉNEAU MAXIMUM DE 2H
                    // =============================================

                    int duree =
                            Math.min(
                                    2,
                                    heuresRestantes
                            );


                    if (
                            heureCourante + duree
                                    > HEURE_FIN
                    ) {

                        heureCourante =
                                HEURE_DEBUT;

                        jourIndex++;


                        if (
                                jourIndex
                                        >= jours.length
                        ) {

                            System.out.println(
                                    "⚠️ Semaine saturée pour "
                                            + classe.getNomComplet()
                            );

                            semaineSaturee =
                                    true;

                            break;
                        }

                        continue;
                    }


                    String jour =
                            jours[jourIndex];


                    // =============================================
                    // 👨‍🏫 PROF
                    // =============================================

                    boolean conflitProf =
                            edtRepo
                                    .existsByEnseignantIdAndAnneeScolaireIdAndJourAndHeureDebutLessThanAndHeureFinGreaterThan(
                                            a.getEnseignant().getId(),
                                            anneeId,
                                            jour,
                                            heureCourante + duree,
                                            heureCourante
                                    );


                    if (conflitProf) {
                        continue;
                    }


                    // =============================================
                    // 👨‍🎓 CLASSE + SOUS-GROUPE
                    // =============================================

                    Long sousGroupeId =
                            programme.getSousGroupe() != null
                                    ? programme.getSousGroupe().getId()
                                    : null;


                    boolean conflitClasse =
                            edtRepo
                                    .existsConflitClasseAvecSousGroupe(
                                            null,
                                            classe.getId(),
                                            sousGroupeId,
                                            anneeId,
                                            jour,
                                            heureCourante + duree,
                                            heureCourante
                                    );


                    if (conflitClasse) {
                        continue;
                    }


                    // =============================================
                    // 🏫 SALLE
                    // =============================================

                    if (salleClasse != null) {

                        boolean conflitSalle =
                                edtRepo
                                        .existsBySalleIdAndAnneeScolaireIdAndJourAndHeureDebutLessThanAndHeureFinGreaterThan(
                                                salleClasse.getId(),
                                                anneeId,
                                                jour,
                                                heureCourante + duree,
                                                heureCourante
                                        );


                        if (conflitSalle) {
                            continue;
                        }
                    }


                    // =============================================
                    // 🔥 CRÉATION
                    // =============================================

                    EmploiDuTemps edt =
                            new EmploiDuTemps();

                    edt.setClasse(classe);

                    edt.setMatiere(
                            programme.getMatiere()
                    );

                    edt.setEnseignant(
                            a.getEnseignant()
                    );

                    edt.setAnneeScolaire(
                            programme.getAnneeScolaire()
                    );

                    edt.setSalle(
                            salleClasse
                    );

                    // 🔥 SOUS-GROUPE
                    edt.setSousGroupe(
                            programme.getSousGroupe()
                    );

                    edt.setJour(
                            jour
                    );

                    edt.setHeureDebut(
                            heureCourante
                    );

                    edt.setHeureFin(
                            heureCourante + duree
                    );


                    edtRepo.save(edt);


                    // =============================================
                    // 🔥 MISE À JOUR HEURES
                    // =============================================

                    heureCourante += duree;

                    restant.put(
                            a.getId(),
                            heuresRestantes - duree
                    );


                    if (
                            restant.get(a.getId())
                                    <= 0
                    ) {

                        it.remove();
                    }


                    if (
                            heureCourante
                                    >= HEURE_FIN
                    ) {

                        heureCourante =
                                HEURE_DEBUT;

                        jourIndex++;


                        if (
                                jourIndex
                                        >= jours.length
                        ) {

                            System.out.println(
                                    "⚠️ Semaine terminée pour "
                                            + classe.getNomComplet()
                            );

                            semaineSaturee =
                                    true;

                            break;
                        }
                    }
                }
            }
        }
    }
}