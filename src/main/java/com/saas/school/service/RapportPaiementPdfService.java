
        package com.saas.school.service;

import com.saas.school.dto.LigneFraisDTO;
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
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RapportPaiementPdfService {

    private static final float MARGE = 40;
    private static final float HAUTEUR_LIGNE = 24;

    private static final float TAILLE_TITRE = 18;
    private static final float TAILLE_SECTION = 12;
    private static final float TAILLE_TEXTE = 8;
    private static final float TAILLE_HEADER = 7;

    private static final float BAS_PAGE = 55;
    private static final float HAUT_PAGE = 50;

    /**
     * Génère le PDF complet de facture / état de compte.
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

            // =====================================================
            // PREMIÈRE PAGE
            // =====================================================

            PDPage page = nouvellePage(document);

            float y =
                    PDRectangle.A4.getHeight()
                            - HAUT_PAGE;

            // =====================================================
            // EN-TÊTE
            // =====================================================

            try (
                    PDPageContentStream cs =
                            new PDPageContentStream(
                                    document,
                                    page
                            )
            ) {

                y = dessinerTitre(
                        cs,
                        fontBold,
                        y
                );

                y -= 25;

                y = dessinerInformationsEleve(
                        cs,
                        fontRegular,
                        fontBold,
                        rapport,
                        y
                );
            }

            // =====================================================
            // FRAIS
            // =====================================================

            ResultatTableau resultatFrais =
                    dessinerTableauFrais(
                            document,
                            page,
                            fontRegular,
                            fontBold,
                            rapport.getFrais(),
                            y - 15
                    );

            page = resultatFrais.page();
            y = resultatFrais.y();

            // =====================================================
            // HISTORIQUE DES PAIEMENTS
            // =====================================================

            ResultatSection resultatPaiements =
                    dessinerSectionPaiements(
                            document,
                            page,
                            fontRegular,
                            fontBold,
                            rapport.getPaiements(),
                            y
                    );

            page = resultatPaiements.page();
            y = resultatPaiements.y();

            // =====================================================
            // RÉSUMÉ
            // =====================================================

            ResultatSection resultatResume =
                    dessinerResume(
                            document,
                            page,
                            fontRegular,
                            fontBold,
                            rapport,
                            y
                    );

            page = resultatResume.page();

            // =====================================================
            // PIED DE PAGE SUR TOUTES LES PAGES
            // =====================================================

            dessinerPiedsDePage(
                    document,
                    fontRegular
            );

            // =====================================================
            // SAUVEGARDE
            // =====================================================

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            document.save(output);

            return output.toByteArray();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erreur lors de la génération de la facture PDF",
                    e
            );
        }
    }


    // =========================================================
    // TITRE
    // =========================================================

    private float dessinerTitre(
            PDPageContentStream cs,
            PDType0Font bold,
            float y
    ) throws IOException {

        cs.beginText();

        cs.setFont(
                bold,
                TAILLE_TITRE
        );

        cs.newLineAtOffset(
                MARGE,
                y
        );

        cs.showText(
                "FACTURE DE SCOLARITÉ"
        );

        cs.endText();

        y -= 12;

        ligneHorizontale(
                cs,
                MARGE,
                PDRectangle.A4.getWidth() - MARGE,
                y
        );

        return y;
    }


    // =========================================================
    // INFORMATIONS ÉLÈVE
    // =========================================================

    private float dessinerInformationsEleve(
            PDPageContentStream cs,
            PDType0Font regular,
            PDType0Font bold,
            RapportPaiementDTO rapport,
            float y
    ) throws IOException {

        y = ecrireLigne(
                cs,
                regular,
                bold,
                MARGE,
                y,
                "Élève",
                rapport.getNomEleve()
        );

        y = ecrireLigne(
                cs,
                regular,
                bold,
                MARGE,
                y,
                "Matricule",
                rapport.getMatricule()
        );

        y = ecrireLigne(
                cs,
                regular,
                bold,
                MARGE,
                y,
                "Classe",
                rapport.getClasse()
        );

        y = ecrireLigne(
                cs,
                regular,
                bold,
                MARGE,
                y,
                "Année scolaire",
                rapport.getAnneeScolaire()
        );

        return y;
    }


    // =========================================================
    // TABLEAU DES FRAIS
    // =========================================================

    private ResultatTableau dessinerTableauFrais(
            PDDocument document,
            PDPage page,
            PDType0Font regular,
            PDType0Font bold,
            List<LigneFraisDTO> frais,
            float y
    ) throws IOException {

        float largeurPage =
                PDRectangle.A4.getWidth();

        float x = MARGE;

        float largeurTableau =
                largeurPage - (2 * MARGE);

        float[] largeurs = {
                120,
                75,
                80,
                80,
                80,
                70
        };

        String[] titres = {
                "Frais",
                "Période",
                "Montant",
                "Payé",
                "Reste",
                "Statut"
        };

        // =====================================================
        // TITRE DE SECTION
        // =====================================================

        if (y < 130) {

            page = nouvellePage(document);

            y = hauteurDisponible(page) - 20;
        }

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

            cs.setFont(
                    bold,
                    TAILLE_SECTION
            );

            cs.newLineAtOffset(
                    MARGE,
                    y
            );

            cs.showText(
                    "SITUATION DES FRAIS"
            );

            cs.endText();

            y -= 12;

            ligneHorizontale(
                    cs,
                    MARGE,
                    largeurPage - MARGE,
                    y
            );

            y -= 15;
        }

        // =====================================================
        // EN-TÊTE
        // =====================================================

        dessinerEnteteTableau(
                document,
                page,
                x,
                y,
                largeurTableau,
                largeurs,
                titres,
                bold
        );

        y -= HAUTEUR_LIGNE;

        // =====================================================
        // AUCUN FRAIS
        // =====================================================

        if (frais == null || frais.isEmpty()) {

            dessinerLigneVide(
                    document,
                    page,
                    regular,
                    x,
                    y,
                    largeurTableau,
                    "Aucun frais enregistré."
            );

            y -= HAUTEUR_LIGNE;

            return new ResultatTableau(
                    page,
                    y
            );
        }

        // =====================================================
        // LIGNES
        // =====================================================

        for (LigneFraisDTO fraisDTO : frais) {

            // Nouvelle page avant de dessiner la ligne
            if (y < BAS_PAGE + HAUTEUR_LIGNE) {

                page = nouvellePage(document);

                y =
                        PDRectangle.A4.getHeight()
                                - HAUT_PAGE;

                dessinerEnteteTableau(
                        document,
                        page,
                        x,
                        y,
                        largeurTableau,
                        largeurs,
                        titres,
                        bold
                );

                y -= HAUTEUR_LIGNE;
            }

            dessinerRectangle(
                    document,
                    page,
                    x,
                    y - HAUTEUR_LIGNE,
                    largeurTableau,
                    HAUTEUR_LIGNE
            );

            String periode =
                    construirePeriode(
                            fraisDTO
                    );

            String statut =
                    fraisDTO.getStatutPaiement() != null
                            ? traduireStatut(
                            fraisDTO.getStatutPaiement()
                    )
                            : "-";

            String[] valeurs = {

                    valeur(
                            fraisDTO.getTypeFraisLibelle()
                    ),

                    periode,

                    formatMontant(
                            fraisDTO.getMontantTotal()
                    ),

                    formatMontant(
                            fraisDTO.getMontantPaye()
                    ),

                    formatMontant(
                            fraisDTO.getResteAPayer()
                    ),

                    statut
            };

            dessinerCellules(
                    document,
                    page,
                    regular,
                    x,
                    y,
                    largeurs,
                    valeurs
            );

            y -= HAUTEUR_LIGNE;
        }

        return new ResultatTableau(
                page,
                y
        );
    }


    // =========================================================
    // SECTION PAIEMENTS
    // =========================================================

    private ResultatSection dessinerSectionPaiements(
            PDDocument document,
            PDPage page,
            PDType0Font regular,
            PDType0Font bold,
            List<RapportPaiementDTO.LignePaiementDTO> paiements,
            float y
    ) throws IOException {

        float largeurPage =
                PDRectangle.A4.getWidth();

        float x = MARGE;

        float largeurTableau =
                largeurPage - (2 * MARGE);

        float[] largeurs = {
                90,
                65,
                105,
                75,
                85,
                85
        };

        String[] titres = {
                "Référence",
                "Date",
                "Frais",
                "Période",
                "Mode",
                "Montant"
        };

        // =====================================================
        // ESPACE NÉCESSAIRE
        // =====================================================

        if (y < 170) {

            page = nouvellePage(document);

            y =
                    PDRectangle.A4.getHeight()
                            - HAUT_PAGE;
        } else {

            y -= 25;
        }

        // =====================================================
        // TITRE
        // =====================================================

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

            cs.setFont(
                    bold,
                    TAILLE_SECTION
            );

            cs.newLineAtOffset(
                    MARGE,
                    y
            );

            cs.showText(
                    "HISTORIQUE DES PAIEMENTS"
            );

            cs.endText();

            y -= 12;

            ligneHorizontale(
                    cs,
                    MARGE,
                    largeurPage - MARGE,
                    y
            );

            y -= 15;
        }

        // =====================================================
        // EN-TÊTE
        // =====================================================

        dessinerEnteteTableau(
                document,
                page,
                x,
                y,
                largeurTableau,
                largeurs,
                titres,
                bold
        );

        y -= HAUTEUR_LIGNE;

        // =====================================================
        // AUCUN PAIEMENT
        // =====================================================

        if (paiements == null || paiements.isEmpty()) {

            dessinerLigneVide(
                    document,
                    page,
                    regular,
                    x,
                    y,
                    largeurTableau,
                    "Aucun paiement enregistré."
            );

            y -= HAUTEUR_LIGNE;

            return new ResultatSection(
                    page,
                    y
            );
        }

        // =====================================================
        // LIGNES
        // =====================================================

        for (
                RapportPaiementDTO.LignePaiementDTO paiement
                : paiements
        ) {

            if (y < BAS_PAGE + HAUTEUR_LIGNE) {

                page = nouvellePage(document);

                y =
                        PDRectangle.A4.getHeight()
                                - HAUT_PAGE;

                // Répéter le titre sur la nouvelle page
                try (
                        PDPageContentStream cs =
                                new PDPageContentStream(
                                        document,
                                        page
                                )
                ) {

                    cs.beginText();

                    cs.setFont(
                            bold,
                            10
                    );

                    cs.newLineAtOffset(
                            MARGE,
                            y
                    );

                    cs.showText(
                            "HISTORIQUE DES PAIEMENTS — SUITE"
                    );

                    cs.endText();

                    y -= 15;
                }

                dessinerEnteteTableau(
                        document,
                        page,
                        x,
                        y,
                        largeurTableau,
                        largeurs,
                        titres,
                        bold
                );

                y -= HAUTEUR_LIGNE;
            }

            dessinerRectangle(
                    document,
                    page,
                    x,
                    y - HAUTEUR_LIGNE,
                    largeurTableau,
                    HAUTEUR_LIGNE
            );

            String[] valeurs = {

                    valeur(
                            paiement.getReference()
                    ),

                    valeur(
                            paiement.getDate()
                    ),

                    valeur(
                            paiement.getTypeFrais()
                    ),

                    valeur(
                            paiement.getPeriode()
                    ),

                    valeur(
                            paiement.getModePaiement()
                    ),

                    formatMontant(
                            paiement.getMontant()
                    )
            };

            dessinerCellules(
                    document,
                    page,
                    regular,
                    x,
                    y,
                    largeurs,
                    valeurs
            );

            y -= HAUTEUR_LIGNE;
        }

        return new ResultatSection(
                page,
                y
        );
    }


    // =========================================================
    // RÉSUMÉ
    // =========================================================

    private ResultatSection dessinerResume(
            PDDocument document,
            PDPage page,
            PDType0Font regular,
            PDType0Font bold,
            RapportPaiementDTO rapport,
            float y
    ) throws IOException {

        float largeurPage =
                PDRectangle.A4.getWidth();

        // =====================================================
        // NOUVELLE PAGE SI NÉCESSAIRE
        // =====================================================

        if (y < 170) {

            page = nouvellePage(document);

            y =
                    PDRectangle.A4.getHeight()
                            - HAUT_PAGE;
        }

        y -= 25;

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

            // =================================================
            // TITRE
            // =================================================

            cs.beginText();

            cs.setFont(
                    bold,
                    TAILLE_SECTION
            );

            cs.newLineAtOffset(
                    MARGE,
                    y
            );

            cs.showText(
                    "RÉSUMÉ"
            );

            cs.endText();

            y -= 12;

            ligneHorizontale(
                    cs,
                    MARGE,
                    largeurPage - MARGE,
                    y
            );

            y -= 25;

            // =================================================
            // TOTAUX
            // =================================================

            float xTotal =
                    largeurPage
                            - MARGE
                            - 220;

            y = ecrireTotal(
                    cs,
                    regular,
                    bold,
                    xTotal,
                    y,
                    "Total à payer",
                    rapport.getTotalAPayer()
            );

            y = ecrireTotal(
                    cs,
                    regular,
                    bold,
                    xTotal,
                    y,
                    "Total payé",
                    rapport.getTotalPaye()
            );

            y -= 5;

            y = ecrireTotal(
                    cs,
                    regular,
                    bold,
                    xTotal,
                    y,
                    "Reste à payer",
                    rapport.getResteAPayer()
            );

            y -= 10;

            // =================================================
            // POURCENTAGE
            // =================================================

            cs.beginText();

            cs.setFont(
                    regular,
                    9
            );

            cs.newLineAtOffset(
                    xTotal,
                    y
            );

            cs.showText(
                    "Pourcentage payé : "
                            + String.format(
                            Locale.FRANCE,
                            "%.1f",
                            rapport.getPourcentagePaye()
                    )
                            + " %"
            );

            cs.endText();
        }

        return new ResultatSection(
                page,
                y
        );
    }


    // =========================================================
    // EN-TÊTE TABLEAU
    // =========================================================

    private void dessinerEnteteTableau(
            PDDocument document,
            PDPage page,
            float x,
            float y,
            float largeurTableau,
            float[] largeurs,
            String[] titres,
            PDType0Font bold
    ) throws IOException {

        dessinerRectangle(
                document,
                page,
                x,
                y - HAUTEUR_LIGNE,
                largeurTableau,
                HAUTEUR_LIGNE
        );

        float positionX = x;

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

                cs.setFont(
                        bold,
                        TAILLE_HEADER
                );

                cs.newLineAtOffset(
                        positionX + 4,
                        y - 16
                );

                cs.showText(
                        titres[i]
                );

                cs.endText();

                positionX += largeurs[i];
            }
        }
    }


    // =========================================================
    // CELLULES
    // =========================================================

    private void dessinerCellules(
            PDDocument document,
            PDPage page,
            PDType0Font regular,
            float x,
            float y,
            float[] largeurs,
            String[] valeurs
    ) throws IOException {

        float positionX = x;

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

                String texte =
                        adapterTexteColonne(
                                valeurs[i],
                                largeurs[i]
                        );

                cs.beginText();

                cs.setFont(
                        regular,
                        TAILLE_TEXTE
                );

                cs.newLineAtOffset(
                        positionX + 4,
                        y - 16
                );

                cs.showText(texte);

                cs.endText();

                positionX += largeurs[i];
            }
        }
    }


    // =========================================================
    // LIGNE VIDE
    // =========================================================

    private void dessinerLigneVide(
            PDDocument document,
            PDPage page,
            PDType0Font regular,
            float x,
            float y,
            float largeur,
            String texte
    ) throws IOException {

        dessinerRectangle(
                document,
                page,
                x,
                y - HAUTEUR_LIGNE,
                largeur,
                HAUTEUR_LIGNE
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

            cs.setFont(
                    regular,
                    TAILLE_TEXTE
            );

            cs.newLineAtOffset(
                    x + 5,
                    y - 16
            );

            cs.showText(texte);

            cs.endText();
        }
    }


    // =========================================================
    // LIGNE INFORMATION
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
                valeur(
                        valeur
                )
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
            double montant
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
    // PÉRIODE
    // =========================================================

    private String construirePeriode(
            LigneFraisDTO frais
    ) {

        if (frais == null) {
            return "-";
        }

        // Frais annuel
        if (
                frais.getTypeFraisFrequence() != null
                        &&
                        (
                                frais.getTypeFraisFrequence()
                                        .equalsIgnoreCase("ANNUEL")
                                        ||
                                        frais.getTypeFraisFrequence()
                                                .equalsIgnoreCase("UNIQUE")
                        )
        ) {

            if (frais.getAnnee() != null) {
                return "Annuel " + frais.getAnnee();
            }

            return "Annuel";
        }

        Integer mois =
                frais.getMois();

        Integer annee =
                frais.getAnnee();

        if (mois == null) {

            if (annee != null) {
                return String.valueOf(annee);
            }

            return "-";
        }

        if (mois < 1 || mois > 12) {
            return "-";
        }

        if (annee == null) {
            return NOMS_MOIS[mois];
        }

        return NOMS_MOIS[mois]
                + " "
                + annee;
    }


    // =========================================================
    // STATUT
    // =========================================================

    private String traduireStatut(
            String statut
    ) {

        if (statut == null) {
            return "-";
        }

        return switch (statut.toUpperCase()) {

            case "PAYE" ->
                    "PAYÉ";

            case "PARTIEL" ->
                    "PARTIEL";

            case "NON_PAYE" ->
                    "NON PAYÉ";

            default ->
                    statut;
        };
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

        return formatMontant(
                montant.doubleValue()
        );
    }


    private String formatMontant(
            double montant
    ) {

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
    // TEXTE
    // =========================================================

    private String valeur(
            String texte
    ) {

        if (
                texte == null
                        ||
                        texte.isBlank()
        ) {

            return "-";
        }

        return texte;
    }


    /**
     * Adapte le texte à la largeur de la colonne.
     *
     * On évite de couper systématiquement à 18 caractères.
     */
    private String adapterTexteColonne(
            String texte,
            float largeur
    ) {

        if (texte == null || texte.isBlank()) {
            return "-";
        }

        int longueurMax;

        if (largeur >= 115) {
            longueurMax = 24;
        } else if (largeur >= 90) {
            longueurMax = 19;
        } else if (largeur >= 75) {
            longueurMax = 15;
        } else {
            longueurMax = 12;
        }

        if (texte.length() <= longueurMax) {
            return texte;
        }

        return texte.substring(
                0,
                longueurMax - 1
        ) + "…";
    }


    // =========================================================
    // NOUVELLE PAGE
    // =========================================================

    private PDPage nouvellePage(
            PDDocument document
    ) {

        PDPage page =
                new PDPage(
                        PDRectangle.A4
                );

        document.addPage(page);

        return page;
    }


    // =========================================================
    // HAUTEUR DISPONIBLE
    // =========================================================

    private float hauteurDisponible(
            PDPage page
    ) {

        return page.getMediaBox().getHeight();
    }


    // =========================================================
    // PIED DE PAGE
    // =========================================================

    private void dessinerPiedsDePage(
            PDDocument document,
            PDType0Font regular
    ) throws IOException {

        int nombrePages =
                document.getNumberOfPages();

        for (int i = 0; i < nombrePages; i++) {

            PDPage page =
                    document.getPage(i);

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

                float largeur =
                        page.getMediaBox().getWidth();

                float y = 30;

                ligneHorizontale(
                        cs,
                        MARGE,
                        largeur - MARGE,
                        45
                );

                cs.beginText();

                cs.setFont(
                        regular,
                        7
                );

                cs.newLineAtOffset(
                        MARGE,
                        y
                );

                cs.showText(
                        "Document généré automatiquement."
                );

                cs.endText();

                String pageText =
                        "Page "
                                + (i + 1)
                                + " / "
                                + nombrePages;

                float largeurTexte =
                        regular.getStringWidth(
                                pageText
                        ) / 1000 * 7;

                cs.beginText();

                cs.setFont(
                        regular,
                        7
                );

                cs.newLineAtOffset(
                        largeur
                                - MARGE
                                - largeurTexte,
                        y
                );

                cs.showText(
                        pageText
                );

                cs.endText();
            }
        }
    }


    // =========================================================
    // RESULTATS
    // =========================================================

    private record ResultatTableau(
            PDPage page,
            float y
    ) {
    }


    private record ResultatSection(
            PDPage page,
            float y
    ) {
    }


    // =========================================================
    // MOIS
    // =========================================================

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
