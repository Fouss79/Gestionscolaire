package com.saas.school.service;

import com.saas.school.dto.BulletinDtos;
import com.saas.school.dto.ResultatFinAnneeDto;
import com.saas.school.entity.DecisionConseil;
import com.saas.school.entity.Inscription;

import com.saas.school.repository.InscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ResultatFinAnneePrimaireService {

    private final InscriptionRepository inscriptionRepository;
    private final BulletinPrimaireService bulletinPrimaireService;

    /**
     * Année scolaire primaire :
     * septembre -> août.
     */
    private static final List<String> MOIS_ANNEE = List.of(
            "Septembre",
            "Octobre",
            "Novembre",
            "Décembre",
            "Janvier",
            "Février",
            "Mars",
            "Avril",
            "Mai",
            "Juin",
            "Juillet",
            "Août"
    );

    /**
     * Résultat annuel d'un élève.
     */
    @Transactional(readOnly = true)
    public ResultatFinAnneeDto construirePourEleve(
            Long inscriptionId,
            Long anneeScolaireId
    ) {

        Inscription inscription =
                recupererInscriptionValide(
                        inscriptionId,
                        anneeScolaireId
                );

        Long classeId = inscription.getClasse().getId();

        /*
         * On calcule le classement complet de la classe
         * afin d'obtenir le rang de l'élève.
         */
        List<ResultatTemporaire> resultatsClasse =
                calculerResultatsClasse(
                        classeId,
                        anneeScolaireId
                );

        ResultatTemporaire resultatEleve =
                resultatsClasse.stream()
                        .filter(r ->
                                Objects.equals(
                                        r.inscriptionId(),
                                        inscriptionId
                                )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Résultat annuel introuvable pour l'inscription "
                                                + inscriptionId
                                )
                        );

        int effectifClasse =
                inscriptionRepository
                        .findActifsByClasseAndAnnee(
                                classeId,
                                anneeScolaireId,
                                StatutInscription.VALIDE
                        )
                        .size();

        return construireDto(
                inscription,
                resultatEleve,
                effectifClasse
        );
    }

    /**
     * Résultats annuels de toute une classe.
     */
    @Transactional(readOnly = true)
    public List<ResultatFinAnneeDto> construirePourClasse(
            Long classeId,
            Long anneeScolaireId
    ) {

        List<Inscription> inscriptions =
                inscriptionRepository.findActifsByClasseAndAnnee(
                        classeId,
                        anneeScolaireId,
                        StatutInscription.VALIDE
                );

        System.out.println("====================================");
        System.out.println("RESULTAT FIN ANNEE PRIMAIRE");
        System.out.println("classeId = " + classeId);
        System.out.println("anneeScolaireId = " + anneeScolaireId);
        System.out.println(
                "Inscriptions VALIDE = " + inscriptions.size()
        );
        System.out.println("====================================");

        if (inscriptions.isEmpty()) {
            return List.of();
        }

        /*
         * Calcul du classement complet.
         */
        List<ResultatTemporaire> resultatsClasse =
                calculerResultatsClasse(
                        classeId,
                        anneeScolaireId
                );

        System.out.println(
                "Résultats temporaires = "
                        + resultatsClasse.size()
        );

        Map<Long, ResultatTemporaire> parInscription =
                new HashMap<>();

        for (ResultatTemporaire resultat : resultatsClasse) {
            parInscription.put(
                    resultat.inscriptionId(),
                    resultat
            );
        }

        int effectifClasse = inscriptions.size();

        List<ResultatFinAnneeDto> dtos =
                new ArrayList<>();

        for (Inscription inscription : inscriptions) {

            ResultatTemporaire resultat =
                    parInscription.get(
                            inscription.getId()
                    );

            if (resultat == null) {
                continue;
            }

            dtos.add(
                    construireDto(
                            inscription,
                            resultat,
                            effectifClasse
                    )
            );
        }

        /*
         * Ordre du classement :
         * 1, 2, 2, 4...
         */
        dtos.sort(
                Comparator.comparing(
                        ResultatFinAnneeDto::getRangDansClasse,
                        Comparator.nullsLast(
                                Comparator.naturalOrder()
                        )
                )
        );

        return dtos;
    }

    /**
     * Calcule les moyennes annuelles de toute la classe.
     *
     * Règle primaire :
     *
     * moyenne annuelle =
     * moyenne des moyennes mensuelles disponibles.
     *
     * Exemple :
     *
     * Septembre = 7,50
     * Octobre   = 8,00
     * Novembre  = 6,50
     *
     * moyenne annuelle = 7,33 / 10
     */
    private List<ResultatTemporaire> calculerResultatsClasse(
            Long classeId,
            Long anneeScolaireId
    ) {

        List<Inscription> inscriptions =
                inscriptionRepository.findActifsByClasseAndAnnee(
                        classeId,
                        anneeScolaireId,
                        StatutInscription.VALIDE
                );

        System.out.println("===== RESULTATS ANNUELS =====");
        System.out.println("classeId = " + classeId);
        System.out.println(
                "anneeScolaireId = " + anneeScolaireId
        );
        System.out.println(
                "Inscriptions valides = "
                        + inscriptions.size()
        );

        if (inscriptions.isEmpty()) {
            return List.of();
        }

        /*
         * inscriptionId -> moyennes mensuelles disponibles
         */
        Map<Long, List<BigDecimal>> moyennesParInscription =
                new HashMap<>();

        for (Inscription inscription : inscriptions) {

            moyennesParInscription.put(
                    inscription.getId(),
                    new ArrayList<>()
            );

            System.out.println(
                    "Inscription : id="
                            + inscription.getId()
                            + " | élève="
                            + inscription.getEleve().getNom()
                            + " "
                            + inscription.getEleve().getPrenom()
            );
        }

        /*
         * IMPORTANT :
         * On construit le bulletin de toute la classe
         * une seule fois par mois.
         */
        for (String mois : MOIS_ANNEE) {

            System.out.println(
                    "===== MOIS " + mois + " ====="
            );

            List<BulletinDtos> bulletins =
                    bulletinPrimaireService.construire(
                            classeId,
                            anneeScolaireId,
                            mois
                    );

            System.out.println(
                    "Nombre de bulletins = "
                            + (bulletins == null
                            ? "NULL"
                            : bulletins.size())
            );

            if (bulletins == null) {
                continue;
            }

            for (BulletinDtos bulletin : bulletins) {

                Long inscriptionId =
                        bulletin.inscriptionId();

                System.out.println(
                        "Bulletin : inscriptionId="
                                + inscriptionId
                                + " | élève="
                                + bulletin.eleve()
                                + " | moyenne="
                                + bulletin.moyenne()
                );

                if (inscriptionId == null) {
                    continue;
                }

                if (bulletin.moyenne() == null) {
                    continue;
                }

                List<BigDecimal> moyennes =
                        moyennesParInscription.get(
                                inscriptionId
                        );

                if (moyennes != null) {
                    moyennes.add(
                            bulletin.moyenne()
                    );
                }
            }
        }

        /*
         * Construction des résultats annuels.
         */
        List<ResultatTemporaire> resultats =
                new ArrayList<>();

        for (Inscription inscription : inscriptions) {

            List<BigDecimal> moyennes =
                    moyennesParInscription.get(
                            inscription.getId()
                    );

            Double moyenneAnnuelle =
                    calculerMoyenneAnnuelle(
                            moyennes
                    );

            System.out.println(
                    "ANNÉE : inscriptionId="
                            + inscription.getId()
                            + " | moyenne="
                            + moyenneAnnuelle
                            + " | nombre mois="
                            + (moyennes == null
                            ? 0
                            : moyennes.size())
            );

            /*
             * ICI était le problème principal :
             * les résultats doivent être ajoutés à la liste.
             */
            resultats.add(
                    new ResultatTemporaire(
                            inscription.getId(),
                            moyenneAnnuelle,
                            null
                    )
            );
        }

        /*
         * Tri décroissant des moyennes.
         */
        resultats.sort(
                Comparator.comparing(
                        ResultatTemporaire::moyenne,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );

        /*
         * Classement compétition :
         *
         * 1, 2, 2, 4
         */
        List<ResultatTemporaire> classes =
                new ArrayList<>();

        Double precedente = null;
        int rang = 0;

        for (int i = 0; i < resultats.size(); i++) {

            ResultatTemporaire resultat =
                    resultats.get(i);

            Double moyenne =
                    resultat.moyenne();

            if (moyenne != null) {

                if (precedente == null
                        || Double.compare(
                        moyenne,
                        precedente
                ) != 0) {

                    rang = i + 1;
                }

                precedente = moyenne;
            }

            classes.add(
                    new ResultatTemporaire(
                            resultat.inscriptionId(),
                            moyenne,
                            moyenne == null
                                    ? null
                                    : rang
                    )
            );
        }

        System.out.println(
                "===== FIN CALCUL RESULTATS ====="
        );
        System.out.println(
                "Nombre de résultats = "
                        + classes.size()
        );

        return classes;
    }

    /**
     * Moyenne des moyennes mensuelles disponibles.
     */
    private Double calculerMoyenneAnnuelle(
            List<BigDecimal> moyennes
    ) {

        if (moyennes == null
                || moyennes.isEmpty()) {

            return null;
        }

        BigDecimal somme =
                BigDecimal.ZERO;

        int nombre = 0;

        for (BigDecimal moyenne : moyennes) {

            if (moyenne == null) {
                continue;
            }

            somme = somme.add(moyenne);
            nombre++;
        }

        if (nombre == 0) {
            return null;
        }

        return somme
                .divide(
                        BigDecimal.valueOf(nombre),
                        2,
                        RoundingMode.HALF_UP
                )
                .doubleValue();
    }

    /**
     * Construction du DTO PDF.
     */
    private ResultatFinAnneeDto construireDto(
            Inscription inscription,
            ResultatTemporaire resultat,
            int effectifClasse
    ) {

        ResultatFinAnneeDto dto =
                new ResultatFinAnneeDto();

        dto.setNomEtPrenom(
                inscription.getEleve().getNom()
                        + " "
                        + inscription.getEleve().getPrenom()
        );

        dto.setMatricule(
                inscription.getEleve().getMatricule()
        );

        dto.setClasseNom(
                inscription.getClasse().getNomComplet()
        );

        dto.setEffectifClasse(
                effectifClasse
        );

        if (inscription.getAnneeScolaire() != null) {

            dto.setAnneeDebut(
                    String.valueOf(
                            inscription
                                    .getAnneeScolaire()
                                    .getDateDebut()
                                    .getYear()
                    )
            );

            dto.setAnneeFin(
                    String.valueOf(
                            inscription
                                    .getAnneeScolaire()
                                    .getDateFin()
                                    .getYear()
                    )
            );
        }

        dto.setMoyenneAnnuelle(
                resultat.moyenne()
        );

        dto.setRangDansClasse(
                resultat.rang()
        );

        /*
         * Décision automatique provisoire.
         * On pourra ensuite la rendre manuelle
         * si la décision doit être celle du conseil.
         */
        dto.setDecision(
                determinerDecision(
                        resultat.moyenne()
                )
        );

        dto.setLieu("Bamako");
        dto.setDateEdition(LocalDate.now());

        return dto;
    }

    /**
     * Vérifie qu'une inscription appartient bien
     * à l'année scolaire demandée et est valide.
     */
    private Inscription recupererInscriptionValide(
            Long inscriptionId,
            Long anneeScolaireId
    ) {

        Inscription inscription =
                inscriptionRepository.findById(
                        inscriptionId
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Inscription introuvable : "
                                        + inscriptionId
                        )
                );

        if (inscription.getAnneeScolaire() == null
                || !Objects.equals(
                anneeScolaireId,
                inscription
                        .getAnneeScolaire()
                        .getId()
        )) {

            throw new IllegalArgumentException(
                    "L'inscription "
                            + inscriptionId
                            + " n'appartient pas à l'année scolaire "
                            + anneeScolaireId
            );
        }

        if (inscription.getStatut()
                != StatutInscription.VALIDE) {

            throw new IllegalArgumentException(
                    "L'inscription "
                            + inscriptionId
                            + " n'est pas validée."
            );
        }

        if (inscription.getClasse() == null) {

            throw new IllegalArgumentException(
                    "L'inscription "
                            + inscriptionId
                            + " n'a aucune classe."
            );
        }

        return inscription;
    }

    /**
     * Décision automatique provisoire.
     *
     * À confirmer selon la règle de l'école.
     */
    private DecisionConseil determinerDecision(
            Double moyenne
    ) {

        if (moyenne == null) {
            return null;
        }

        if (moyenne >= 5.0) {
            return DecisionConseil.ADMISSION_CLASSE_SUPERIEURE;
        }

        return DecisionConseil.REDOUBLEMENT;
    }

    private record ResultatTemporaire(
            Long inscriptionId,
            Double moyenne,
            Integer rang
    ) {
    }
}