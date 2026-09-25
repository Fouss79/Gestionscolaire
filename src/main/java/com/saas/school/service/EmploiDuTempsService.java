package com.saas.school.service;

import com.saas.school.dto.EmploiDto;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Gestion des emplois du temps.
 *
 * IMPORTANT :
 * heureDebut et heureFin sont maintenant stockées
 * en MINUTES depuis minuit.
 *
 * Exemples :
 *
 * 08:00 = 480
 * 08:30 = 510
 * 09:00 = 540
 * 10:00 = 600
 */
@Service
@RequiredArgsConstructor
public class EmploiDuTempsService {

    private final ClasseRepository classeRepo;
    private final AffectationEnseignantRepository affectationRepo;
    private final EmploiDuTempsRepository edtRepo;
    private final SalleRepository salleRepo;
    private final EmargementRepository emargementRepo;

    private final SousGroupeRepository sousGroupeRepo;
    private final CoefficientMatiereRepository coefficientRepo;

    // =========================================================
    // JOURS
    // =========================================================

    private final String[] jours = {
            "LUNDI",
            "MARDI",
            "MERCREDI",
            "JEUDI",
            "VENDREDI"
    };

    // =========================================================
    // HORAIRES AUTOMATIQUES
    // =========================================================

    // 08:00
    private static final int HEURE_DEBUT = 8 * 60;

    // 18:00
    private static final int HEURE_FIN = 18 * 60;

    // Durée maximale automatique : 2 heures
    private static final int DUREE_MAX_COURS = 2 * 60;

    // Pas utilisé pour avancer lors d'un créneau bloqué
    private static final int PAS_GENERATION = 30;

    private static final int MINUTES_PAR_HEURE = 60;

    private static final int SECURITE_MAX_ITERATIONS = 50000;


    // =========================================================
    // UTILITAIRES
    // =========================================================

    /**
     * Vérifie qu'un horaire est cohérent.
     */
    private void validerHoraires(int debut, int fin) {

        if (debut < 0 || debut > 24 * 60) {
            throw new RuntimeException(
                    "L'heure de début est invalide"
            );
        }

        if (fin < 0 || fin > 24 * 60) {
            throw new RuntimeException(
                    "L'heure de fin est invalide"
            );
        }

        if (fin <= debut) {
            throw new RuntimeException(
                    "L'heure de fin doit être supérieure "
                            + "à l'heure de début"
            );
        }
    }


    /**
     * Convertit les heures hebdomadaires du programme
     * en minutes.
     *
     * Exemple :
     * 4 heures = 240 minutes
     */
    private int convertirHeuresEnMinutes(Integer heures) {

        if (heures == null) {
            return 0;
        }

        return heures * MINUTES_PAR_HEURE;
    }


    /**
     * Formate une durée en minutes pour les messages.
     *
     * 30  -> 30min
     * 60  -> 1h
     * 90  -> 1h 30min
     * 120 -> 2h
     */
    private String formaterDuree(int minutes) {

        int heures = minutes / MINUTES_PAR_HEURE;
        int minutesRestantes = minutes % MINUTES_PAR_HEURE;

        if (heures > 0 && minutesRestantes > 0) {
            return heures + "h "
                    + minutesRestantes
                    + "min";
        }

        if (heures > 0) {
            return heures + "h";
        }

        return minutesRestantes + "min";
    }


    /**
     * Vérifie si deux Long représentent la même valeur.
     */
    private boolean memeValeur(Long a, Long b) {

        return Objects.equals(a, b);
    }


    // =========================================================
    // RÉCUPÉRER LE SOUS-GROUPE
    // =========================================================

    private SousGroupe getSousGroupe(Long sousGroupeId) {

        if (sousGroupeId == null) {
            return null;
        }

        return sousGroupeRepo.findById(sousGroupeId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Sous-groupe introuvable"
                        ));
    }


    // =========================================================
    // DÉTERMINER LE PROGRAMME APPLICABLE
    // =========================================================

    private CoefficientMatiere obtenirProgrammeApplicable(
            AffectationEnseignant affectation,
            Long sousGroupeId
    ) {

        CoefficientMatiere programmeGeneral =
                affectation.getCoefficientMatiere();

        if (sousGroupeId == null) {
            return programmeGeneral;
        }

        Optional<CoefficientMatiere> coefficientSousGroupe =
                coefficientRepo
                        .findByMatiereIdAndNiveauIdAndAnneeScolaireIdAndClasseIdAndSousGroupeId(
                                programmeGeneral.getMatiere().getId(),
                                programmeGeneral.getNiveau().getId(),
                                programmeGeneral.getAnneeScolaire().getId(),
                                programmeGeneral.getClasse().getId(),
                                sousGroupeId
                        );

        return coefficientSousGroupe
                .orElse(programmeGeneral);
    }


    // =========================================================
    // CALCUL DES MINUTES DÉJÀ PLANIFIÉES
    // =========================================================

    private int heuresDejaPlanifiees(
            Long classeId,
            Long matiereId,
            Long anneeId,
            Long sousGroupeId
    ) {

        if (sousGroupeId != null) {

            Integer total =
                    edtRepo.totalHeuresDejaPlanifieesSousGroupe(
                            classeId,
                            matiereId,
                            anneeId,
                            sousGroupeId
                    );

            return total != null ? total : 0;
        }

        Integer total =
                edtRepo.totalHeuresDejaPlanifiees(
                        classeId,
                        matiereId,
                        anneeId
                );

        return total != null ? total : 0;
    }


    // =========================================================
    // CRÉATION MANUELLE
    // =========================================================

    @Transactional
    public EmploiDuTemps create(EmploiDto dto) {

        return creerCreneauInterne(
                dto,
                null
        );
    }


    // =========================================================
    // MODIFICATION
    // =========================================================

    @Transactional
    public EmploiDuTemps update(
            Long id,
            EmploiDto dto
    ) {

        EmploiDuTemps edt =
                edtRepo.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Créneau introuvable"
                                ));

        // =====================================================
        // VALIDATION HORAIRES
        // =====================================================

        validerHoraires(
                dto.getHeureDebut(),
                dto.getHeureFin()
        );

        Classe classe =
                classeRepo.findById(
                                dto.getClasseId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Classe introuvable"
                                ));


        // =====================================================
        // AFFECTATION
        // =====================================================

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
                                        "Cet enseignant n'est pas affecté "
                                                + "à cette matière dans cette classe "
                                                + "pour cette année"
                                ));


        // =====================================================
        // SOUS-GROUPE
        // =====================================================

        SousGroupe sousGroupe =
                getSousGroupe(
                        dto.getSousGroupeId()
                );


        // =====================================================
        // PROGRAMME
        // =====================================================

        CoefficientMatiere programme =
                obtenirProgrammeApplicable(
                        affectation,
                        dto.getSousGroupeId()
                );


        // =====================================================
        // CALCUL DES MINUTES
        // =====================================================

        int ancienneDuree =
                edt.getHeureFin()
                        - edt.getHeureDebut();

        int nouvelleDuree =
                dto.getHeureFin()
                        - dto.getHeureDebut();


        // =====================================================
        // MINUTES DÉJÀ UTILISÉES
        // =====================================================

        int dejaUtilise =
                heuresDejaPlanifiees(
                        dto.getClasseId(),
                        dto.getMatiereId(),
                        dto.getAnneeId(),
                        dto.getSousGroupeId()
                );


        /*
         * On retire l'ancien créneau uniquement si
         * l'ancien créneau appartient bien au même
         * contexte que celui que l'on modifie.
         *
         * Cela évite de retirer 2h d'une matière A
         * lorsqu'on transforme le créneau en matière B.
         */
        Long ancienneClasseId =
                edt.getClasse() != null
                        ? edt.getClasse().getId()
                        : null;

        Long ancienneMatiereId =
                edt.getMatiere() != null
                        ? edt.getMatiere().getId()
                        : null;

        Long ancienneAnneeId =
                edt.getAnneeScolaire() != null
                        ? edt.getAnneeScolaire().getId()
                        : null;

        Long ancienSousGroupeId =
                edt.getSousGroupe() != null
                        ? edt.getSousGroupe().getId()
                        : null;


        boolean memeContexte =
                memeValeur(
                        ancienneClasseId,
                        dto.getClasseId()
                )
                        && memeValeur(
                        ancienneMatiereId,
                        dto.getMatiereId()
                )
                        && memeValeur(
                        ancienneAnneeId,
                        dto.getAnneeId()
                )
                        && memeValeur(
                        ancienSousGroupeId,
                        dto.getSousGroupeId()
                );


        if (memeContexte) {

            dejaUtilise -= ancienneDuree;

            if (dejaUtilise < 0) {
                dejaUtilise = 0;
            }
        }


        // =====================================================
        // QUOTA HEBDOMADAIRE
        // =====================================================

        int quotaHebdoMinutes =
                convertirHeuresEnMinutes(
                        programme.getNombreHeuresParSemaine()
                );


        int restant =
                quotaHebdoMinutes
                        - dejaUtilise;
        System.out.println(
                "===== DEBUG EMPLOI ====="
        );

        System.out.println(
                "Classe : " + dto.getClasseId()
        );

        System.out.println(
                "Matière : " + dto.getMatiereId()
        );

        System.out.println(
                "Sous-groupe : " + dto.getSousGroupeId()
        );

        System.out.println(
                "Nombre heures/semaine : "
                        + programme.getNombreHeuresParSemaine()
        );

        System.out.println(
                "Quota minutes : "
                        + quotaHebdoMinutes
        );

        System.out.println(
                "Minutes déjà planifiées : "
                        + dejaUtilise
        );

        System.out.println(
                "Nouvelle durée : "
                        + nouvelleDuree
        );

        System.out.println(
                "Restant : "
                        + restant
        );

        System.out.println(
                "========================"
        );

        if (nouvelleDuree <= 0) {

            throw new RuntimeException(
                    "La durée du créneau doit être "
                            + "supérieure à 0"
            );
        }


        if (nouvelleDuree > restant) {

            throw new RuntimeException(
                    "Heures insuffisantes pour "
                            + (
                            sousGroupe != null
                                    ? "le sous-groupe"
                                    : "la matière"
                    )
                            + ". Restant : "
                            + formaterDuree(restant)
            );
        }


        // =====================================================
        // SALLE
        // =====================================================

        Salle salle = null;

        if (dto.getSalleId() != null) {

            salle =
                    salleRepo.findById(
                                    dto.getSalleId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Salle introuvable"
                                    ));

        } else if (classe.getSalle() != null) {

            salle = classe.getSalle();
        }


        // =====================================================
        // CONFLITS
        // =====================================================

        verifierConflits(
                id,
                dto.getEnseignantId(),
                dto.getClasseId(),
                dto.getSousGroupeId(),
                salle != null
                        ? salle.getId()
                        : null,
                dto.getAnneeId(),
                dto.getJour(),
                dto.getHeureDebut(),
                dto.getHeureFin()
        );


        // =====================================================
        // MODIFICATION
        // =====================================================

        edt.setClasse(classe);

        edt.setMatiere(
                programme.getMatiere()
        );

        edt.setEnseignant(
                affectation.getEnseignant()
        );

        edt.setAnneeScolaire(
                programme.getAnneeScolaire()
        );

        edt.setSalle(salle);

        edt.setSousGroupe(
                sousGroupe
        );

        edt.setJour(
                dto.getJour()
        );

        edt.setHeureDebut(
                dto.getHeureDebut()
        );

        edt.setHeureFin(
                dto.getHeureFin()
        );


        return edtRepo.save(edt);
    }


    // =========================================================
    // LOGIQUE COMMUNE CREATE / UPDATE
    // =========================================================

    private EmploiDuTemps creerCreneauInterne(
            EmploiDto dto,
            Long idEnCoursDeModification
    ) {

        // =====================================================
        // VALIDATION HORAIRES
        // =====================================================

        validerHoraires(
                dto.getHeureDebut(),
                dto.getHeureFin()
        );


        // =====================================================
        // CLASSE
        // =====================================================

        Classe classe =
                classeRepo.findById(
                                dto.getClasseId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Classe introuvable"
                                ));


        // =====================================================
        // AFFECTATION
        // =====================================================

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
                                        "Cet enseignant n'est pas affecté "
                                                + "à cette matière dans cette classe "
                                                + "pour cette année"
                                ));


        // =====================================================
        // SOUS-GROUPE
        // =====================================================

        SousGroupe sousGroupe =
                getSousGroupe(
                        dto.getSousGroupeId()
                );


        // =====================================================
        // PROGRAMME
        // =====================================================

        CoefficientMatiere programme =
                obtenirProgrammeApplicable(
                        affectation,
                        dto.getSousGroupeId()
                );


        // =====================================================
        // MINUTES DÉJÀ UTILISÉES
        // =====================================================

        int dejaUtilise =
                heuresDejaPlanifiees(
                        dto.getClasseId(),
                        dto.getMatiereId(),
                        dto.getAnneeId(),
                        dto.getSousGroupeId()
                );


        // =====================================================
        // MODIFICATION
        // =====================================================

        if (idEnCoursDeModification != null) {

            EmploiDuTemps ancien =
                    edtRepo.findById(
                                    idEnCoursDeModification
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Créneau à modifier "
                                                    + "introuvable"
                                    ));


            boolean memeContexte =
                    memeValeur(
                            ancien.getClasse() != null
                                    ? ancien.getClasse().getId()
                                    : null,
                            dto.getClasseId()
                    )
                            && memeValeur(
                            ancien.getMatiere() != null
                                    ? ancien.getMatiere().getId()
                                    : null,
                            dto.getMatiereId()
                    )
                            && memeValeur(
                            ancien.getAnneeScolaire() != null
                                    ? ancien.getAnneeScolaire().getId()
                                    : null,
                            dto.getAnneeId()
                    )
                            && memeValeur(
                            ancien.getSousGroupe() != null
                                    ? ancien.getSousGroupe().getId()
                                    : null,
                            dto.getSousGroupeId()
                    );


            if (memeContexte) {

                int ancienneDuree =
                        ancien.getHeureFin()
                                - ancien.getHeureDebut();

                dejaUtilise -= ancienneDuree;

                if (dejaUtilise < 0) {
                    dejaUtilise = 0;
                }
            }
        }


        // =====================================================
        // QUOTA
        // =====================================================

        int quotaHebdoMinutes =
                convertirHeuresEnMinutes(
                        programme.getNombreHeuresParSemaine()
                );


        int restant =
                quotaHebdoMinutes
                        - dejaUtilise;
        System.out.println("===== DEBUG EMPLOI CREATE =====");

        System.out.println(
                "Classe : " + dto.getClasseId()
        );

        System.out.println(
                "Matière : " + dto.getMatiereId()
        );

        System.out.println(
                "Sous-groupe : " + dto.getSousGroupeId()
        );

        System.out.println(
                "Nombre heures/semaine : "
                        + programme.getNombreHeuresParSemaine()
        );

        System.out.println(
                "Quota minutes : "
                        + quotaHebdoMinutes
        );

        System.out.println(
                "Minutes déjà planifiées : "
                        + dejaUtilise
        );

        System.out.println(
                "Nouvelle durée : "
                        + (dto.getHeureFin() - dto.getHeureDebut())
        );

        System.out.println(
                "Restant : "
                        + restant
        );

        System.out.println("==============================");

        if (restant <= 0) {

            throw new RuntimeException(
                    "Toutes les heures prévues pour "
                            + (
                            sousGroupe != null
                                    ? "ce sous-groupe"
                                    : "cette matière"
                    )
                            + " sont déjà planifiées"
            );
        }


        // =====================================================
        // DURÉE
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
                    "Heures insuffisantes. "
                            + "Restant : "
                            + formaterDuree(restant)
            );
        }


        // =====================================================
        // SALLE
        // =====================================================

        Salle salle = null;

        if (dto.getSalleId() != null) {

            salle =
                    salleRepo.findById(
                                    dto.getSalleId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Salle introuvable"
                                    ));

        } else if (classe.getSalle() != null) {

            salle = classe.getSalle();
        }


        // =====================================================
        // CONFLITS
        // =====================================================

        verifierConflits(
                idEnCoursDeModification,
                dto.getEnseignantId(),
                dto.getClasseId(),
                dto.getSousGroupeId(),
                salle != null
                        ? salle.getId()
                        : null,
                dto.getAnneeId(),
                dto.getJour(),
                dto.getHeureDebut(),
                dto.getHeureFin()
        );


        // =====================================================
        // CRÉATION / MODIFICATION
        // =====================================================

        EmploiDuTemps edt;

        if (idEnCoursDeModification != null) {

            edt =
                    edtRepo.findById(
                                    idEnCoursDeModification
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Créneau à modifier "
                                                    + "introuvable"
                                    ));

        } else {

            edt = new EmploiDuTemps();
        }


        // =====================================================
        // DONNÉES
        // =====================================================

        edt.setClasse(classe);

        edt.setMatiere(
                programme.getMatiere()
        );

        edt.setEnseignant(
                affectation.getEnseignant()
        );

        edt.setAnneeScolaire(
                programme.getAnneeScolaire()
        );

        edt.setSalle(salle);

        edt.setSousGroupe(
                sousGroupe
        );

        edt.setJour(
                dto.getJour()
        );

        edt.setHeureDebut(
                dto.getHeureDebut()
        );

        edt.setHeureFin(
                dto.getHeureFin()
        );


        return edtRepo.save(edt);
    }


    // =========================================================
    // CONFLITS
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
        // VALIDATION
        // =====================================================

        validerHoraires(
                debut,
                fin
        );


        // =====================================================
        // PROFESSEUR
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
        // CLASSE + SOUS-GROUPE
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
        // SALLE
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
    // CONSULTATION
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
    // SUPPRESSION
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
    // GÉNÉRATION AUTOMATIQUE
    // =========================================================

    @Transactional
    public void generer(Long anneeId) {

        // Supprimer l'ancien emploi du temps
        edtRepo.deleteByAnneeScolaireId(
                anneeId
        );


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
            // POOL
            // =================================================

            List<AffectationEnseignant> pool =
                    new ArrayList<>(
                            affectations
                    );


            pool.sort((a, b) ->
                    Integer.compare(
                            b.getCoefficientMatiere()
                                    .getCoefficient(),

                            a.getCoefficientMatiere()
                                    .getCoefficient()
                    )
            );


            // =================================================
            // MINUTES RESTANTES
            // =================================================

            Map<Long, Integer> restant =
                    new HashMap<>();


            for (AffectationEnseignant a : pool) {

                CoefficientMatiere programme =
                        a.getCoefficientMatiere();


                Integer heures =
                        programme
                                .getNombreHeuresParSemaine();


                int minutes =
                        convertirHeuresEnMinutes(
                                heures
                        );


                restant.put(
                        a.getId(),
                        minutes
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
            // GÉNÉRATION
            // =================================================

            while (
                    !pool.isEmpty()
                            && !semaineSaturee
            ) {

                securite++;


                if (
                        securite
                                > SECURITE_MAX_ITERATIONS
                ) {

                    System.out.println(
                            "❌ Sécurité boucle globale déclenchée pour "
                                    + classe.getNomComplet()
                    );

                    break;
                }


                Iterator<AffectationEnseignant> it =
                        pool.iterator();


                boolean placementEffectue =
                        false;


                while (it.hasNext()) {

                    AffectationEnseignant a =
                            it.next();


                    CoefficientMatiere programme =
                            a.getCoefficientMatiere();


                    int minutesRestantes =
                            restant.getOrDefault(
                                    a.getId(),
                                    0
                            );


                    if (minutesRestantes <= 0) {

                        it.remove();

                        continue;
                    }


                    // =============================================
                    // DURÉE MAXIMUM : 2 HEURES
                    // =============================================

                    int duree =
                            Math.min(
                                    DUREE_MAX_COURS,
                                    minutesRestantes
                            );


                    // =============================================
                    // FIN DE JOURNÉE
                    // =============================================

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
                    // PROF
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
                    // SOUS-GROUPE
                    // =============================================

                    Long sousGroupeId =
                            programme.getSousGroupe() != null
                                    ? programme
                                    .getSousGroupe()
                                    .getId()
                                    : null;


                    // =============================================
                    // CLASSE
                    // =============================================

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
                    // SALLE
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
                    // CRÉATION
                    // =============================================

                    EmploiDuTemps edt =
                            new EmploiDuTemps();


                    edt.setClasse(
                            classe
                    );


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
                    // MISE À JOUR
                    // =============================================

                    heureCourante += duree;


                    restant.put(
                            a.getId(),
                            minutesRestantes - duree
                    );


                    placementEffectue =
                            true;


                    if (
                            restant.get(
                                    a.getId()
                            ) <= 0
                    ) {

                        it.remove();
                    }


                    // =============================================
                    // FIN DE JOURNÉE
                    // =============================================

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


                // =================================================
                // PROTECTION CONTRE UNE BOUCLE BLOQUÉE
                // =================================================
                /*
                 * Si aucune matière n'a pu être placée,
                 * on avance de 30 minutes.
                 *
                 * Cela évite de rester bloqué éternellement
                 * sur un créneau occupé par un professeur,
                 * une classe ou une salle.
                 */

                if (
                        !placementEffectue
                                && !semaineSaturee
                ) {

                    heureCourante +=
                            PAS_GENERATION;


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
                                    "⚠️ Impossible de placer "
                                            + "tous les cours pour "
                                            + classe.getNomComplet()
                            );

                            semaineSaturee =
                                    true;
                        }
                    }
                }
            }
        }
    }
}
