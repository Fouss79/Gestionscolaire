package com.saas.school.service;

import com.saas.school.dto.RapportPaiementDTO;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RapportPaiementPdfService {

    private static final float MARGE = 40;

    /**
     * Génère le PDF à partir du rapport.
     */
    public byte[] genererPdf(RapportPaiementDTO rapport) {

        try (
                PDDocument document = new PDDocument();
                InputStream regularStream =
                        new ClassPathResource(
                                "fonts/DejaVuSans.ttf"
                        ).getInputStream();
                InputStream boldStream =
                        new ClassPathResource(
                                "fonts/DejaVuSans-Bold.ttf"
                        ).getInputStream()
        ) {

            // ==============================
            // POLICES
            // ==============================

            PDType0Font fontRegular =
                    PDType0Font.load(
                            document,
                            regularStream
                    );

            PDType0Font fontBold =
                    PDType0Font.load(
                            document,
                            boldStream
                    );

            // ==============================
            // PAGE
            // ==============================

            PDPage page =
                    new PDPage(PDRectangle.A4);

            document.addPage(page);

            float largeur =
                    PDRectangle.A4.getWidth();

            float hauteur =
                    PDRectangle.A4.getHeight();

            float y = hauteur - 50;

            try (
                    PDPageContentStream cs =
                            new PDPageContentStream(
                                    document,
                                    page
                            )
            ) {

                // ==============================
                // TITRE
                // ==============================

                cs.beginText();
                cs.setFont(fontBold, 18);
                cs.newLineAtOffset(MARGE, y);

                cs.showText(
                        "RAPPORT DES PAIEMENTS"
                );

                cs.endText();

                y -= 35;

                // Ligne
                ligneHorizontale(
                        cs,
                        MARGE,
                        largeur - MARGE,
                        y
                );

                y -= 30;

                // ==============================
                // INFORMATIONS ÉLÈVE
                // ==============================

                y = ecrireLigne(
                        cs,
                        fontRegular,
                        fontBold,
                        MARGE,
                        y,
                        "Élève",
                        rapport.getNomEleve()
                );

                y = ecrireLigne(
                        cs,
                        fontRegular,
                        fontBold,
                        MARGE,
                        y,
                        "Matricule",
                        rapport.getMatricule()
                );

                y = ecrireLigne(
                        cs,
                        fontRegular,
                        fontBold,
                        MARGE,
                        y,
                        "Classe",
                        rapport.getClasse()
                );

                y = ecrireLigne(
                        cs,
                        fontRegular,
                        fontBold,
                        MARGE,
                        y,
                        "Année scolaire",
                        rapport.getAnneeScolaire()
                );

                y -= 15;

                // ==============================
                // TABLEAU
                // ==============================

                y = dessinerTableau(
                        document,
                        page,
                        fontRegular,
                        fontBold,
                        rapport,
                        y
                );

                // ==============================
                // TOTAUX
                // ==============================

                y -= 25;

                float xTotal =
                        largeur - MARGE - 210;

                y = ecrireTotal(
                        cs,
                        fontRegular,
                        fontBold,
                        xTotal,
                        y,
                        "Total à payer",
                        rapport.getTotalAPayer()
                );

                y = ecrireTotal(
                        cs,
                        fontRegular,
                        fontBold,
                        xTotal,
                        y,
                        "Total payé",
                        rapport.getTotalPaye()
                );

                y = ecrireTotal(
                        cs,
                        fontRegular,
                        fontBold,
                        xTotal,
                        y,
                        "Reste à payer",
                        rapport.getResteAPayer()
                );

                // ==============================
                // PIED DE PAGE
                // ==============================

                cs.beginText();
                cs.setFont(fontRegular, 8);
                cs.newLineAtOffset(
                        MARGE,
                        35
                );

                cs.showText(
                        "Document généré automatiquement."
                );

                cs.endText();
            }

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            document.save(output);

            return output.toByteArray();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erreur lors de la génération du rapport PDF",
                    e
            );
        }
    }


    // =========================================================
    // TABLEAU
    // =========================================================

    private float dessinerTableau(
            PDDocument document,
            PDPage page,
            PDType0Font regular,
            PDType0Font bold,
            RapportPaiementDTO rapport,
            float y
    ) throws IOException {

        float largeur =
                PDRectangle.A4.getWidth();

        float x = MARGE;

        float largeurTableau =
                largeur - (2 * MARGE);

        float hauteurLigne = 25;

        /*
         * Colonnes :
         *
         * Référence
         * Date
         * Type de frais
         * Période
         * Mode
         * Montant
         */

        float[] largeurs = {
                70,
                55,
                95,
                70,
                75,
                80
        };

        // ==============================
        // EN-TÊTE
        // ==============================

        dessinerRectangle(
                document,
                page,
                x,
                y - hauteurLigne,
                largeurTableau,
                hauteurLigne
        );

        float positionX = x;

        String[] titres = {
                "Référence",
                "Date",
                "Type de frais",
                "Période",
                "Mode",
                "Montant"
        };

        try (
                PDPageContentStream cs =
                        new PDPageContentStream(
                                document,
                                page,
                                PDPageContentStream.AppendMode.APPEND,
                                true,
                                true
                        )
        ) {

            for (int i = 0; i < titres.length; i++) {

                cs.beginText();
                cs.setFont(bold, 7);
                cs.newLineAtOffset(
                        positionX + 4,
                        y - 16
                );

                cs.showText(
                        tronquer(
                                titres[i],
                                16
                        )
                );

                cs.endText();

                positionX += largeurs[i];
            }
        }

        y -= hauteurLigne;

        // ==============================
        // LIGNES
        // ==============================

        if (
                rapport.getPaiements() == null ||
                        rapport.getPaiements().isEmpty()
        ) {

            dessinerRectangle(
                    document,
                    page,
                    x,
                    y - hauteurLigne,
                    largeurTableau,
                    hauteurLigne
            );

            try (
                    PDPageContentStream cs =
                            new PDPageContentStream(
                                    document,
                                    page,
                                    PDPageContentStream.AppendMode.APPEND,
                                    true,
                                    true
                            )
            ) {

                cs.beginText();
                cs.setFont(regular, 8);
                cs.newLineAtOffset(
                        x + 5,
                        y - 16
                );

                cs.showText(
                        "Aucun paiement enregistré."
                );

                cs.endText();
            }

            return y - hauteurLigne;
        }

        for (
                RapportPaiementDTO.LignePaiementDTO paiement
                : rapport.getPaiements()
        ) {

            /*
             * Si on arrive trop bas,
             * on crée une nouvelle page.
             */

            if (y < 90) {

                page =
                        new PDPage(
                                PDRectangle.A4
                        );

                document.addPage(page);

                y =
                        PDRectangle.A4.getHeight()
                                - 50;
            }

            dessinerRectangle(
                    document,
                    page,
                    x,
                    y - hauteurLigne,
                    largeurTableau,
                    hauteurLigne
            );

            positionX = x;

            String[] valeurs = {

                    paiement.getReference(),

                    paiement.getDate(),

                    paiement.getTypeFrais(),

                    paiement.getPeriode(),

                    paiement.getModePaiement(),

                    formatMontant(
                            paiement.getMontant()
                    )
            };

            try (
                    PDPageContentStream cs =
                            new PDPageContentStream(
                                    document,
                                    page,
                                    PDPageContentStream.AppendMode.APPEND,
                                    true,
                                    true
                            )
            ) {

                for (int i = 0; i < valeurs.length; i++) {

                    cs.beginText();
                    cs.setFont(
                            regular,
                            7
                    );

                    cs.newLineAtOffset(
                            positionX + 4,
                            y - 16
                    );

                    cs.showText(
                            tronquer(
                                    valeurs[i],
                                    18
                            )
                    );

                    cs.endText();

                    positionX += largeurs[i];
                }
            }

            y -= hauteurLigne;
        }

        return y;
    }


    // =========================================================
    // LIGNE INFORMATIONS
    // =========================================================

    private float ecrireLigne(
            PDPageContentStream cs,
            PDType0Font regular,
            PDType0Font bold,
            float x,
            float y,
            String label,
            String valeur
    ) throws IOException {

        cs.beginText();
        cs.setFont(
                regular,
                9
        );

        cs.newLineAtOffset(
                x,
                y
        );

        cs.showText(
                label + " :"
        );

        cs.endText();


        cs.beginText();
        cs.setFont(
                bold,
                9
        );

        cs.newLineAtOffset(
                x + 130,
                y
        );

        cs.showText(
                valeur != null && !valeur.isBlank()
                        ? valeur
                        : "-"
        );

        cs.endText();

        return y - 18;
    }


    // =========================================================
    // TOTAL
    // =========================================================

    private float ecrireTotal(
            PDPageContentStream cs,
            PDType0Font regular,
            PDType0Font bold,
            float x,
            float y,
            String label,
            Double montant
    ) throws IOException {

        cs.beginText();
        cs.setFont(
                regular,
                9
        );

        cs.newLineAtOffset(
                x,
                y
        );

        cs.showText(
                label + " :"
        );

        cs.endText();


        cs.beginText();
        cs.setFont(
                bold,
                9
        );

        cs.newLineAtOffset(
                x + 100,
                y
        );

        cs.showText(
                formatMontant(montant)
        );

        cs.endText();

        return y - 18;
    }


    // =========================================================
    // RECTANGLE
    // =========================================================

    private void dessinerRectangle(
            PDDocument document,
            PDPage page,
            float x,
            float y,
            float largeur,
            float hauteur
    ) throws IOException {

        try (
                PDPageContentStream cs =
                        new PDPageContentStream(
                                document,
                                page,
                                PDPageContentStream.AppendMode.APPEND,
                                true,
                                true
                        )
        ) {

            cs.setStrokingColor(
                    0.75f,
                    0.75f,
                    0.75f
            );

            cs.addRect(
                    x,
                    y,
                    largeur,
                    hauteur
            );

            cs.stroke();
        }
    }


    // =========================================================
    // LIGNE HORIZONTALE
    // =========================================================

    private void ligneHorizontale(
            PDPageContentStream cs,
            float x1,
            float x2,
            float y
    ) throws IOException {

        cs.setStrokingColor(
                0.75f,
                0.75f,
                0.75f
        );

        cs.moveTo(
                x1,
                y
        );

        cs.lineTo(
                x2,
                y
        );

        cs.stroke();
    }


    // =========================================================
    // MONTANT
    // =========================================================

    private String formatMontant(
            Double montant
    ) {

        if (montant == null) {
            return "0 FCFA";
        }

        return String.format(
                Locale.FRANCE,
                "%,.0f FCFA",
                montant
        ).replace(
                ",",
                " "
        );
    }


    // =========================================================
    // TRONCATURE
    // =========================================================

    private String tronquer(
            String texte,
            int longueur
    ) {

        if (texte == null || texte.isBlank()) {
            return "-";
        }

        if (texte.length() <= longueur) {
            return texte;
        }

        return texte.substring(
                0,
                longueur - 1
        ) + "…";
    }
}
