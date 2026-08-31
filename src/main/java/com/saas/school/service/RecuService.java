
        package com.saas.school.service;

import com.saas.school.entity.Ecole;
import com.saas.school.entity.Eleve;
import com.saas.school.entity.Inscription;
import com.saas.school.entity.LigneFrais;
import com.saas.school.entity.PaiementScolarite;
import com.saas.school.repository.PaiementScolariteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RecuService {

    private final PaiementScolariteRepository paiementScolariteRepository;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy 'à' HH:mm",
                    Locale.FRENCH
            );

    public byte[] genererRecuPdf(Long paiementId) {

        PaiementScolarite paiement =
                paiementScolariteRepository.findById(paiementId)
                        .orElseThrow(() ->
                                new RuntimeException("Paiement introuvable"));

        LigneFrais ligne = paiement.getLigneFrais();

        if (ligne == null) {
            throw new RuntimeException(
                    "Ligne de frais introuvable pour ce paiement"
            );
        }

        Inscription inscription = ligne.getInscription();

        if (inscription == null) {
            throw new RuntimeException(
                    "Inscription introuvable pour ce paiement"
            );
        }

        Eleve eleve = inscription.getEleve();
        Ecole ecole = inscription.getEcole();

        try (PDDocument document = new PDDocument()) {

            // =====================================================
            // POLICES UNICODE
            // =====================================================

            PDFont FONT_REGULAR;
            PDFont FONT_BOLD;

            try (
                    InputStream regularStream =
                            new ClassPathResource(
                                    "fonts/DejaVuSans.ttf"
                            ).getInputStream();

                    InputStream boldStream =
                            new ClassPathResource(
                                    "fonts/DejaVuSans-Bold.ttf"
                            ).getInputStream()
            ) {

                FONT_REGULAR =
                        PDType0Font.load(document, regularStream);

                FONT_BOLD =
                        PDType0Font.load(document, boldStream);
            }

            // =====================================================
            // PAGE
            // =====================================================

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margeGauche = 55;
            float largeurPage = PDRectangle.A4.getWidth();
            float y = PDRectangle.A4.getHeight() - 60;

            try (
                    PDPageContentStream cs =
                            new PDPageContentStream(document, page)
            ) {

                // =================================================
                // EN-TÊTE ÉCOLE
                // =================================================

                cs.beginText();
                cs.setFont(FONT_BOLD, 16);
                cs.newLineAtOffset(margeGauche, y);

                cs.showText(
                        ecole != null && ecole.getNom() != null
                                ? nettoyerTexte(ecole.getNom())
                                : "École"
                );

                cs.endText();

                y -= 30;

                // =================================================
                // TITRE
                // =================================================

                cs.beginText();
                cs.setFont(FONT_BOLD, 14);
                cs.newLineAtOffset(margeGauche, y);
                cs.showText("REÇU DE PAIEMENT");
                cs.endText();

                y -= 15;

                // =================================================
                // LIGNE
                // =================================================

                cs.setStrokingColor(
                        0.78f,
                        0.78f,
                        0.78f
                );

                cs.moveTo(
                        margeGauche,
                        y
                );

                cs.lineTo(
                        largeurPage - margeGauche,
                        y
                );

                cs.stroke();

                y -= 30;

                // =================================================
                // RÉFÉRENCE
                // =================================================

                y = ecrireLigne(
                        cs,
                        FONT_REGULAR,
                        FONT_BOLD,
                        margeGauche,
                        y,
                        "N° reçu",
                        paiement.getReference()
                );

                // =================================================
                // DATE
                // =================================================

                y = ecrireLigne(
                        cs,
                        FONT_REGULAR,
                        FONT_BOLD,
                        margeGauche,
                        y,
                        "Date",
                        paiement.getDatePaiement() != null
                                ? paiement.getDatePaiement()
                                .format(DATE_FORMAT)
                                : "-"
                );

                y -= 15;

                // =================================================
                // ÉLÈVE
                // =================================================

                String nomEleve =
                        (eleve != null && eleve.getPrenom() != null
                                ? eleve.getPrenom()
                                : "")
                                + " "
                                +
                                (eleve != null && eleve.getNom() != null
                                        ? eleve.getNom()
                                        : "");

                y = ecrireLigne(
                        cs,
                        FONT_REGULAR,
                        FONT_BOLD,
                        margeGauche,
                        y,
                        "Élève",
                        nomEleve.trim()
                );

                // =================================================
                // CLASSE
                // =================================================

                y = ecrireLigne(
                        cs,
                        FONT_REGULAR,
                        FONT_BOLD,
                        margeGauche,
                        y,
                        "Classe",
                        inscription.getClasse() != null
                                ? inscription.getClasse().getNomComplet()
                                : "-"
                );

                // =================================================
                // ANNÉE SCOLAIRE
                // =================================================

                if (inscription.getAnneeScolaire() != null) {

                    y = ecrireLigne(
                            cs,
                            FONT_REGULAR,
                            FONT_BOLD,
                            margeGauche,
                            y,
                            "Année scolaire",
                            inscription
                                    .getAnneeScolaire()
                                    .getNom()
                    );
                }

                y -= 15;

                // =================================================
                // TYPE DE FRAIS
                // =================================================

                y = ecrireLigne(
                        cs,
                        FONT_REGULAR,
                        FONT_BOLD,
                        margeGauche,
                        y,
                        "Type de frais",
                        ligne.getTypeFrais() != null
                                ? ligne.getTypeFrais().getLibelle()
                                : "-"
                );

                // =================================================
                // PÉRIODE
                // =================================================

                if (
                        paiement.getMois() != null
                                && paiement.getAnnee() != null
                ) {

                    String mois =
                            paiement.getMois() >= 1
                                    && paiement.getMois() <= 12
                                    ? NOMS_MOIS[paiement.getMois()]
                                    : "-";

                    y = ecrireLigne(
                            cs,
                            FONT_REGULAR,
                            FONT_BOLD,
                            margeGauche,
                            y,
                            "Période",
                            mois + " " + paiement.getAnnee()
                    );
                }

                // =================================================
                // MODE DE PAIEMENT
                // =================================================

                y = ecrireLigne(
                        cs,
                        FONT_REGULAR,
                        FONT_BOLD,
                        margeGauche,
                        y,
                        "Mode de paiement",
                        libelleMode(
                                paiement.getModePaiement()
                        )
                );

                y -= 20;

                // =================================================
                // MONTANT
                // =================================================

                cs.setStrokingColor(
                        0.86f,
                        0.86f,
                        0.86f
                );

                cs.addRect(
                        margeGauche,
                        y - 35,
                        largeurPage - 2 * margeGauche,
                        45
                );

                cs.stroke();

                cs.beginText();
                cs.setFont(FONT_REGULAR, 11);
                cs.newLineAtOffset(
                        margeGauche + 15,
                        y - 12
                );
                cs.showText("Montant payé");
                cs.endText();

                cs.beginText();
                cs.setFont(FONT_BOLD, 18);
                cs.newLineAtOffset(
                        margeGauche + 15,
                        y - 30
                );

                cs.showText(
                        formatMontant(paiement.getMontant())
                );

                cs.endText();

                // =================================================
                // PIED DE PAGE
                // =================================================

                cs.beginText();
                cs.setFont(FONT_REGULAR, 9);
                cs.newLineAtOffset(
                        margeGauche,
                        50
                );

                cs.showText(
                        "Ce reçu a été généré automatiquement " +
                                "et fait office de preuve de paiement."
                );

                cs.endText();
            }

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            document.save(out);

            return out.toByteArray();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erreur lors de la génération du reçu PDF",
                    e
            );
        }
    }

    private float ecrireLigne(
            PDPageContentStream cs,
            PDFont fontRegular,
            PDFont fontBold,
            float x,
            float y,
            String label,
            String valeur
    ) throws IOException {

        cs.beginText();

        cs.setFont(fontRegular, 10);

        cs.newLineAtOffset(x, y);

        cs.showText(
                nettoyerTexte(label) + " :"
        );

        cs.endText();

        cs.beginText();

        cs.setFont(fontBold, 10);

        cs.newLineAtOffset(
                x + 140,
                y
        );

        cs.showText(
                nettoyerTexte(
                        valeur != null && !valeur.isBlank()
                                ? valeur
                                : "-"
                )
        );

        cs.endText();

        return y - 20;
    }

    private String formatMontant(Double montant) {

        if (montant == null) {
            return "-";
        }

        return String.format(
                        Locale.FRANCE,
                        "%,.0f FCFA",
                        montant
                )
                // Remplace toutes les formes d'espace Unicode
                // par une espace normale.
                .replace('\u202F', ' ')
                .replace('\u00A0', ' ')
                .replace(",", " ");
    }

    private String nettoyerTexte(String texte) {

        if (texte == null) {
            return "";
        }

        return texte
                .replace('\u202F', ' ')
                .replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u2009', ' ');
    }

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

            case "VIREMENT" ->
                    "Virement";

            case "CHEQUE" ->
                    "Chèque";

            default ->
                    code;
        };
    }

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

