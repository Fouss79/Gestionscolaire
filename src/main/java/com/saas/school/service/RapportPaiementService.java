package com.saas.school.service;

import com.saas.school.dto.RapportPaiementDTO;
import com.saas.school.entity.Eleve;
import com.saas.school.entity.Inscription;
import com.saas.school.entity.LigneFrais;
import com.saas.school.entity.PaiementScolarite;
import com.saas.school.repository.InscriptionRepository;
import com.saas.school.repository.LigneFraisRepository;
import com.saas.school.repository.PaiementScolariteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RapportPaiementService {

    private final PaiementScolariteRepository paiementScolariteRepository;
    private final LigneFraisRepository ligneFraisRepository;
    private final InscriptionRepository inscriptionRepository;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy",
                    Locale.FRENCH
            );

    // =====================================================
    // GÉNÉRER LE RAPPORT
    // =====================================================

    public RapportPaiementDTO genererRapport(Long inscriptionId) {

        Inscription inscription =
                inscriptionRepository.findById(inscriptionId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Inscription introuvable"
                                )
                        );

        RapportPaiementDTO rapport =
                new RapportPaiementDTO();

        // =================================================
        // INFORMATIONS ÉLÈVE
        // =================================================

        Eleve eleve = inscription.getEleve();

        if (eleve != null) {

            String prenom =
                    eleve.getPrenom() != null
                            ? eleve.getPrenom()
                            : "";

            String nom =
                    eleve.getNom() != null
                            ? eleve.getNom()
                            : "";

            rapport.setNomEleve(
                    (prenom + " " + nom).trim()
            );

        } else {

            rapport.setNomEleve("-");

        }

        // =================================================
        // CLASSE
        // =================================================

        rapport.setClasse(
                inscription.getClasse() != null
                        ? inscription
                        .getClasse()
                        .getNomComplet()
                        : "-"
        );

        // =================================================
        // ANNÉE SCOLAIRE
        // =================================================

        rapport.setAnneeScolaire(
                inscription.getAnneeScolaire() != null
                        ? inscription
                        .getAnneeScolaire()
                        .getNom()
                        : "-"
        );

        // =================================================
        // LIGNES DE FRAIS
        // =================================================

        List<LigneFrais> lignesFrais =
                ligneFraisRepository
                        .findByInscription_Id(
                                inscriptionId
                        );

        double totalAPayer = 0.0;

        for (LigneFrais ligne : lignesFrais) {

            if (ligne == null) {
                continue;
            }

            if (ligne.getMontantTotal() != null) {

                totalAPayer +=
                        ligne.getMontantTotal();
            }
        }

        // =================================================
        // PAIEMENTS
        // =================================================

        List<PaiementScolarite> paiements =
                paiementScolariteRepository
                        .findByLigneFrais_Inscription_IdOrderByDatePaiementAsc(
                                inscriptionId
                        );

        double totalPaye = 0.0;

        for (PaiementScolarite paiement : paiements) {

            if (paiement == null) {
                continue;
            }

            RapportPaiementDTO.LignePaiementDTO ligne =
                    new RapportPaiementDTO.LignePaiementDTO();

            // ---------------------------------------------
            // RÉFÉRENCE
            // ---------------------------------------------

            ligne.setReference(
                    paiement.getReference() != null
                            ? paiement.getReference()
                            : "-"
            );

            // ---------------------------------------------
            // DATE
            // ---------------------------------------------

            ligne.setDate(
                    paiement.getDatePaiement() != null
                            ? paiement
                            .getDatePaiement()
                            .format(DATE_FORMAT)
                            : "-"
            );

            // ---------------------------------------------
            // TYPE DE FRAIS
            // ---------------------------------------------

            LigneFrais ligneFrais =
                    paiement.getLigneFrais();

            if (ligneFrais != null
                    && ligneFrais.getTypeFrais() != null) {

                ligne.setTypeFrais(
                        ligneFrais
                                .getTypeFrais()
                                .getLibelle()
                );

            } else {

                ligne.setTypeFrais("-");

            }

            // ---------------------------------------------
            // PÉRIODE
            // ---------------------------------------------

            ligne.setPeriode(
                    construirePeriode(paiement)
            );

            // ---------------------------------------------
            // MONTANT
            // ---------------------------------------------

            double montant =
                    paiement.getMontant() != null
                            ? paiement.getMontant()
                            : 0.0;

            ligne.setMontant(montant);

            // ---------------------------------------------
            // MODE DE PAIEMENT
            // ---------------------------------------------

            ligne.setModePaiement(
                    libelleMode(
                            paiement.getModePaiement()
                    )
            );

            totalPaye += montant;

            rapport.getPaiements().add(ligne);
        }

        // =================================================
        // RÉSUMÉ
        // =================================================

        double resteAPayer =
                Math.max(
                        0.0,
                        totalAPayer - totalPaye
                );

        double pourcentagePaye = 0.0;

        if (totalAPayer > 0) {

            pourcentagePaye =
                    (totalPaye / totalAPayer) * 100;

            pourcentagePaye =
                    Math.min(
                            100.0,
                            pourcentagePaye
                    );
        }

        rapport.setTotalAPayer(totalAPayer);
        rapport.setTotalPaye(totalPaye);
        rapport.setResteAPayer(resteAPayer);
        rapport.setPourcentagePaye(pourcentagePaye);
        rapport.setMatricule(
                eleve != null && eleve.getMatricule() != null
                        ? eleve.getMatricule()
                        : "-"
        );

        return rapport;
    }

    // =====================================================
    // PÉRIODE
    // =====================================================

    private String construirePeriode(
            PaiementScolarite paiement
    ) {

        if (paiement.getMois() == null
                || paiement.getAnnee() == null) {

            return "-";
        }

        Integer mois =
                paiement.getMois();

        if (mois < 1 || mois > 12) {
            return "-";
        }

        return NOMS_MOIS[mois]
                + " "
                + paiement.getAnnee();
    }

    // =====================================================
    // MODE DE PAIEMENT
    // =====================================================

    private String libelleMode(String code) {

        if (code == null) {
            return "-";
        }

        return switch (code.toUpperCase()) {

            case "CASH" ->
                    "Espèces";

            case "ORANGE_MONEY" ->
                    "Orange Money";

            case "MOOV_MONEY" ->
                    "Moov Money";

            case "WAVE" ->
                    "Wave";

            case "VIREMENT" ->
                    "Virement";

            case "CHEQUE" ->
                    "Chèque";

            default ->
                    code;
        };
    }

    // =====================================================
    // MOIS
    // =====================================================

    private static final String[] NOMS_MOIS = {

            "",

            "Janvier",
            "Février",
            "Mars",
            "Avril",
            "Mai",
            "Juin",
            "Juillet",
            "Août",
            "Septembre",
            "Octobre",
            "Novembre",
            "Décembre"
    };
}