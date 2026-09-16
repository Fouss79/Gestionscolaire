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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Génère un PDF professionnel de facture / état de compte de scolarité.
 *
 * <p>Corrections apportées par rapport à la version précédente :
 * <ul>
 *     <li>{@code dessinerTitre} ne compilait pas : elle référençait un champ
 *     {@code fontBold} et une variable {@code rapport} qui n'existaient pas
 *     dans sa portée. La méthode reçoit désormais {@code rapport} en
 *     paramètre et utilise la police {@code bold} passée en argument.</li>
 * </ul>
 *
 * <p>Améliorations visuelles : palette de couleurs cohérente, bandeau
 * d'en-tête coloré pour les tableaux, lignes alternées pour la lisibilité,
 * meilleure hiérarchie typographique et pied de page horodaté.
 */
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

    // =========================================================
    // PALETTE DE COULEURS
    // =========================================================

    private static final float[] COULEUR_PRIMAIRE = {0.11f, 0.25f, 0.45f};   // bleu profond
    private static final float[] COULEUR_PRIMAIRE_CLAIRE = {0.90f, 0.93f, 0.97f};
    private static final float[] COULEUR_BORDURE = {0.80f, 0.80f, 0.82f};
    private static final float[] COULEUR_TEXTE = {0.15f, 0.15f, 0.17f};
    private static final float[] COULEUR_TEXTE_SECONDAIRE = {0.42f, 0.44f, 0.48f};
    private static final float[] COULEUR_LIGNE_ALT = {0.97f, 0.97f, 0.98f};
    private static final float[] COULEUR_BLANC = {1f, 1f, 1f};

    private static final float[] COULEUR_STATUT_PAYE = {0.13f, 0.55f, 0.28f};
    private static final float[] COULEUR_STATUT_PARTIEL = {0.80f, 0.55f, 0.10f};
    private static final float[] COULEUR_STATUT_NON_PAYE = {0.75f, 0.18f, 0.18f};

    private static final DateTimeFormatter FORMAT_DATE_GENERATION =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRANCE);

    /**
     * Génère le PDF complet de facture / état de compte.
     */
    public byte[] genererPdf(RapportPaiementDTO rapport) {

        try (
                PDDocument document = new PDDocument();

                InputStream regularStream =
                        new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream();

                InputStream boldStream =
                        new ClassPathResource("fonts/DejaVuSans-Bold.ttf").getInputStream()
        ) {

            PDType0Font fontRegular = PDType0Font.load(document, regularStream);
            PDType0Font fontBold = PDType0Font.load(document, boldStream);

            // =====================================================
            // PREMIÈRE PAGE
            // =====================================================

            PDPage page = nouvellePage(document);

            float y = PDRectangle.A4.getHeight() - HAUT_PAGE;

            // =====================================================
            // EN-TÊTE
            // =====================================================

            try (
                    PDPageContentStream cs = new PDPageContentStream(document, page)
            ) {

                y = dessinerTitre(cs, fontBold, rapport, y);

                y -= 25;

                y = dessinerInformationsEleve(cs, fontRegular, fontBold, rapport, y);
            }

            // =====================================================
            // FRAIS
            // =====================================================

            ResultatTableau resultatFrais =
                    dessinerTableauFrais(document, page, fontRegular, fontBold, rapport.getFrais(), y - 15);

            page = resultatFrais.page();
            y = resultatFrais.y();

            // =====================================================
            // HISTORIQUE DES PAIEMENTS
            // =====================================================

            ResultatSection resultatPaiements =
                    dessinerSectionPaiements(document, page, fontRegular, fontBold, rapport.getPaiements(), y);

            page = resultatPaiements.page();
            y = resultatPaiements.y();

            // =====================================================
            // RÉSUMÉ
            // =====================================================

            ResultatSection resultatResume =
                    dessinerResume(document, page, fontRegular, fontBold, rapport, y);

            page = resultatResume.page();

            // =====================================================
            // PIED DE PAGE SUR TOUTES LES PAGES
            // =====================================================

            dessinerPiedsDePage(document, fontRegular);

            // =====================================================
            // SAUVEGARDE
            // =====================================================

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération de la facture PDF", e);
        }
    }


    // =========================================================
    // TITRE
    // =========================================================

    /**
     * Dessine l'en-tête de la facture : nom de l'établissement, titre du
     * document et filet de séparation.
     *
     * <p>Corrigé : la méthode reçoit désormais {@code bold} et
     * {@code rapport} en paramètres explicites au lieu de référencer des
     * variables inexistantes.
     */
    private float dessinerTitre(
            PDPageContentStream cs,
            PDType0Font bold,
            RapportPaiementDTO rapport,
            float y
    ) throws IOException {

        // =================================================
        // NOM DE L'ÉTABLISSEMENT
        // =================================================

        setCouleurTexte(cs, COULEUR_PRIMAIRE);

        cs.beginText();
        cs.setFont(bold, 16);
        cs.newLineAtOffset(MARGE, y);
        cs.showText(
                rapport.getNomEtablissement() != null && !rapport.getNomEtablissement().isBlank()
                        ? rapport.getNomEtablissement()
                        : "Établissement scolaire"
        );
        cs.endText();

        y -= 25;

        // =================================================
        // TITRE
        // =================================================

        setCouleurTexte(cs, COULEUR_TEXTE);

        cs.beginText();
        cs.setFont(bold, TAILLE_TITRE);
        cs.newLineAtOffset(MARGE, y);
        cs.showText("FACTURE DE SCOLARITÉ");
        cs.endText();

        y -= 12;

        ligneHorizontale(cs, MARGE, PDRectangle.A4.getWidth() - MARGE, y, COULEUR_PRIMAIRE, 1.4f);

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

        y = ecrireLigne(cs, regular, bold, MARGE, y, "Élève", rapport.getNomEleve());
        y = ecrireLigne(cs, regular, bold, MARGE, y, "Matricule", rapport.getMatricule());
        y = ecrireLigne(cs, regular, bold, MARGE, y, "Classe", rapport.getClasse());
        y = ecrireLigne(cs, regular, bold, MARGE, y, "Année scolaire", rapport.getAnneeScolaire());

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

        float largeurPage = PDRectangle.A4.getWidth();
        float x = MARGE;
        float largeurTableau = largeurPage - (2 * MARGE);

        float[] largeurs = {120, 75, 80, 80, 80, 70};
        String[] titres = {"Frais", "Période", "Montant", "Payé", "Reste", "Statut"};

        if (y < 130) {
            page = nouvellePage(document);
            y = hauteurDisponible(page) - 20;
        }

        y = dessinerTitreSection(document, page, "SITUATION DES FRAIS", largeurPage, bold, y);

        dessinerEnteteTableau(document, page, x, y, largeurTableau, largeurs, titres, bold);
        y -= HAUTEUR_LIGNE;

        if (frais == null || frais.isEmpty()) {
            dessinerLigneVide(document, page, regular, x, y, largeurTableau, "Aucun frais enregistré.", false);
            y -= HAUTEUR_LIGNE;
            return new ResultatTableau(page, y);
        }

        boolean ligneAlternee = false;

        for (LigneFraisDTO fraisDTO : frais) {

            if (y < BAS_PAGE + HAUTEUR_LIGNE) {

                page = nouvellePage(document);
                y = PDRectangle.A4.getHeight() - HAUT_PAGE;

                dessinerEnteteTableau(document, page, x, y, largeurTableau, largeurs, titres, bold);
                y -= HAUTEUR_LIGNE;
                ligneAlternee = false;
            }

            if (ligneAlternee) {
                dessinerRectangleRempli(document, page, x, y - HAUTEUR_LIGNE, largeurTableau, HAUTEUR_LIGNE, COULEUR_LIGNE_ALT);
            }

            dessinerRectangle(document, page, x, y - HAUTEUR_LIGNE, largeurTableau, HAUTEUR_LIGNE);

            String periode = construirePeriode(fraisDTO);
            String statut = fraisDTO.getStatutPaiement() != null
                    ? traduireStatut(fraisDTO.getStatutPaiement())
                    : "-";

            String[] valeurs = {
                    valeur(fraisDTO.getTypeFraisLibelle()),
                    periode,
                    formatMontant(fraisDTO.getMontantTotal()),
                    formatMontant(fraisDTO.getMontantPaye()),
                    formatMontant(fraisDTO.getResteAPayer()),
                    statut
            };

            dessinerCellules(document, page, regular, bold, x, y, largeurs, valeurs, statut);

            y -= HAUTEUR_LIGNE;
            ligneAlternee = !ligneAlternee;
        }

        return new ResultatTableau(page, y);
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

        float largeurPage = PDRectangle.A4.getWidth();
        float x = MARGE;
        float largeurTableau = largeurPage - (2 * MARGE);

        float[] largeurs = {90, 65, 105, 75, 85, 85};
        String[] titres = {"Référence", "Date", "Frais", "Période", "Mode", "Montant"};

        if (y < 170) {
            page = nouvellePage(document);
            y = PDRectangle.A4.getHeight() - HAUT_PAGE;
        } else {
            y -= 25;
        }

        y = dessinerTitreSection(document, page, "HISTORIQUE DES PAIEMENTS", largeurPage, bold, y);

        dessinerEnteteTableau(document, page, x, y, largeurTableau, largeurs, titres, bold);
        y -= HAUTEUR_LIGNE;

        if (paiements == null || paiements.isEmpty()) {
            dessinerLigneVide(document, page, regular, x, y, largeurTableau, "Aucun paiement enregistré.", false);
            y -= HAUTEUR_LIGNE;
            return new ResultatSection(page, y);
        }

        boolean ligneAlternee = false;

        for (RapportPaiementDTO.LignePaiementDTO paiement : paiements) {

            if (y < BAS_PAGE + HAUTEUR_LIGNE) {

                page = nouvellePage(document);
                y = PDRectangle.A4.getHeight() - HAUT_PAGE;

                y = dessinerTitreSection(document, page, "HISTORIQUE DES PAIEMENTS — SUITE", largeurPage, bold, y + 25);

                dessinerEnteteTableau(document, page, x, y, largeurTableau, largeurs, titres, bold);
                y -= HAUTEUR_LIGNE;
                ligneAlternee = false;
            }

            if (ligneAlternee) {
                dessinerRectangleRempli(document, page, x, y - HAUTEUR_LIGNE, largeurTableau, HAUTEUR_LIGNE, COULEUR_LIGNE_ALT);
            }

            dessinerRectangle(document, page, x, y - HAUTEUR_LIGNE, largeurTableau, HAUTEUR_LIGNE);

            String[] valeurs = {
                    valeur(paiement.getReference()),
                    valeur(paiement.getDate()),
                    valeur(paiement.getTypeFrais()),
                    valeur(paiement.getPeriode()),
                    valeur(paiement.getModePaiement()),
                    formatMontant(paiement.getMontant())
            };

            dessinerCellules(document, page, regular, bold, x, y, largeurs, valeurs, null);

            y -= HAUTEUR_LIGNE;
            ligneAlternee = !ligneAlternee;
        }

        return new ResultatSection(page, y);
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

        float largeurPage = PDRectangle.A4.getWidth();

        if (y < 170) {
            page = nouvellePage(document);
            y = PDRectangle.A4.getHeight() - HAUT_PAGE;
        }

        y -= 25;

        // Bloc résumé encadré, aligné à droite
        float largeurBloc = 260;
        float xBloc = largeurPage - MARGE - largeurBloc;
        float hauteurBloc = 110;

        try (
                PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)
        ) {

            setCouleurTexte(cs, COULEUR_TEXTE);

            cs.beginText();
            cs.setFont(bold, TAILLE_SECTION);
            cs.newLineAtOffset(MARGE, y);
            cs.showText("RÉSUMÉ");
            cs.endText();

            y -= 12;

            ligneHorizontale(cs, MARGE, largeurPage - MARGE, y, COULEUR_BORDURE, 0.75f);

            y -= 20;
        }

        // Fond du bloc résumé
        dessinerRectangleRempli(document, page, xBloc, y - hauteurBloc + 20, largeurBloc, hauteurBloc, COULEUR_PRIMAIRE_CLAIRE);
        dessinerRectangle(document, page, xBloc, y - hauteurBloc + 20, largeurBloc, hauteurBloc);

        try (
                PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)
        ) {

            float xTexte = xBloc + 16;
            float yCourant = y;

            yCourant = ecrireTotal(cs, regular, bold, xTexte, yCourant, "Total à payer", rapport.getTotalAPayer(), COULEUR_TEXTE);
            yCourant = ecrireTotal(cs, regular, bold, xTexte, yCourant, "Total payé", rapport.getTotalPaye(), COULEUR_STATUT_PAYE);

            yCourant -= 4;
            ligneHorizontale(cs, xTexte, xBloc + largeurBloc - 16, yCourant + 12, COULEUR_BORDURE, 0.5f);

            yCourant = ecrireTotal(cs, regular, bold, xTexte, yCourant, "Reste à payer", rapport.getResteAPayer(), COULEUR_STATUT_NON_PAYE);

            yCourant -= 10;

            setCouleurTexte(cs, COULEUR_TEXTE_SECONDAIRE);

            cs.beginText();
            cs.setFont(regular, 9);
            cs.newLineAtOffset(xTexte, yCourant);
            cs.showText(
                    "Pourcentage payé : "
                            + String.format(Locale.FRANCE, "%.1f", rapport.getPourcentagePaye())
                            + " %"
            );
            cs.endText();

            y = yCourant - 30;
        }

        return new ResultatSection(page, y);
    }


    // =========================================================
    // TITRE DE SECTION (SITUATION DES FRAIS / HISTORIQUE...)
    // =========================================================

    private float dessinerTitreSection(
            PDDocument document,
            PDPage page,
            String titre,
            float largeurPage,
            PDType0Font bold,
            float y
    ) throws IOException {

        try (
                PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)
        ) {

            setCouleurTexte(cs, COULEUR_PRIMAIRE);

            cs.beginText();
            cs.setFont(bold, TAILLE_SECTION);
            cs.newLineAtOffset(MARGE, y);
            cs.showText(titre);
            cs.endText();

            y -= 12;

            ligneHorizontale(cs, MARGE, largeurPage - MARGE, y, COULEUR_PRIMAIRE, 1f);

            y -= 15;
        }

        return y;
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

        dessinerRectangleRempli(document, page, x, y - HAUTEUR_LIGNE, largeurTableau, HAUTEUR_LIGNE, COULEUR_PRIMAIRE);
        dessinerRectangle(document, page, x, y - HAUTEUR_LIGNE, largeurTableau, HAUTEUR_LIGNE);

        float positionX = x;

        try (
                PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)
        ) {

            setCouleurTexte(cs, COULEUR_BLANC);

            for (int i = 0; i < titres.length; i++) {

                cs.beginText();
                cs.setFont(bold, TAILLE_HEADER);
                cs.newLineAtOffset(positionX + 4, y - 16);
                cs.showText(titres[i].toUpperCase(Locale.FRANCE));
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
            PDType0Font bold,
            float x,
            float y,
            float[] largeurs,
            String[] valeurs,
            String statutColonneStatut
    ) throws IOException {

        float positionX = x;

        try (
                PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)
        ) {

            for (int i = 0; i < valeurs.length; i++) {

                String texte = adapterTexteColonne(valeurs[i], largeurs[i]);

                boolean estColonneStatut = statutColonneStatut != null && i == valeurs.length - 1;

                if (estColonneStatut) {
                    setCouleurTexte(cs, couleurPourStatut(valeurs[i]));
                    cs.beginText();
                    cs.setFont(bold, TAILLE_TEXTE);
                } else {
                    setCouleurTexte(cs, COULEUR_TEXTE);
                    cs.beginText();
                    cs.setFont(regular, TAILLE_TEXTE);
                }

                cs.newLineAtOffset(positionX + 4, y - 16);
                cs.showText(texte);
                cs.endText();

                positionX += largeurs[i];
            }
        }
    }

    private float[] couleurPourStatut(String statut) {

        if (statut == null) {
            return COULEUR_TEXTE;
        }

        return switch (statut.toUpperCase(Locale.FRANCE)) {
            case "PAYÉ" -> COULEUR_STATUT_PAYE;
            case "PARTIEL" -> COULEUR_STATUT_PARTIEL;
            case "NON PAYÉ" -> COULEUR_STATUT_NON_PAYE;
            default -> COULEUR_TEXTE;
        };
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
            String texte,
            boolean fond
    ) throws IOException {

        if (fond) {
            dessinerRectangleRempli(document, page, x, y - HAUTEUR_LIGNE, largeur, HAUTEUR_LIGNE, COULEUR_LIGNE_ALT);
        }

        dessinerRectangle(document, page, x, y - HAUTEUR_LIGNE, largeur, HAUTEUR_LIGNE);

        try (
                PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)
        ) {

            setCouleurTexte(cs, COULEUR_TEXTE_SECONDAIRE);

            cs.beginText();
            cs.setFont(regular, TAILLE_TEXTE);
            cs.newLineAtOffset(x + 5, y - 16);
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
            String valeurChamp
    ) throws IOException {

        setCouleurTexte(cs, COULEUR_TEXTE_SECONDAIRE);

        cs.beginText();
        cs.setFont(regular, 9);
        cs.newLineAtOffset(x, y);
        cs.showText(label + " :");
        cs.endText();

        setCouleurTexte(cs, COULEUR_TEXTE);

        cs.beginText();
        cs.setFont(bold, 9);
        cs.newLineAtOffset(x + 130, y);
        cs.showText(valeur(valeurChamp));
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
            double montant,
            float[] couleurMontant
    ) throws IOException {

        setCouleurTexte(cs, COULEUR_TEXTE_SECONDAIRE);

        cs.beginText();
        cs.setFont(regular, 9);
        cs.newLineAtOffset(x, y);
        cs.showText(label + " :");
        cs.endText();

        setCouleurTexte(cs, couleurMontant);

        cs.beginText();
        cs.setFont(bold, 10);
        cs.newLineAtOffset(x + 110, y);
        cs.showText(formatMontant(montant));
        cs.endText();

        return y - 20;
    }


    // =========================================================
    // RECTANGLES
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
                PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)
        ) {

            cs.setStrokingColor(COULEUR_BORDURE[0], COULEUR_BORDURE[1], COULEUR_BORDURE[2]);
            cs.addRect(x, y, largeur, hauteur);
            cs.stroke();
        }
    }

    private void dessinerRectangleRempli(
            PDDocument document,
            PDPage page,
            float x,
            float y,
            float largeur,
            float hauteur,
            float[] couleur
    ) throws IOException {

        try (
                PDPageContentStream cs = new PDPageContentStream(
                        document, page, PDPageContentStream.AppendMode.APPEND, true, true)
        ) {

            cs.setNonStrokingColor(couleur[0], couleur[1], couleur[2]);
            cs.addRect(x, y, largeur, hauteur);
            cs.fill();
        }
    }


    // =========================================================
    // LIGNE HORIZONTALE
    // =========================================================

    private void ligneHorizontale(
            PDPageContentStream cs,
            float x1,
            float x2,
            float y,
            float[] couleur,
            float epaisseur
    ) throws IOException {

        cs.setStrokingColor(couleur[0], couleur[1], couleur[2]);
        cs.setLineWidth(epaisseur);
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
        cs.setLineWidth(1f);
    }

    private void setCouleurTexte(PDPageContentStream cs, float[] couleur) throws IOException {
        cs.setNonStrokingColor(couleur[0], couleur[1], couleur[2]);
    }


    // =========================================================
    // PÉRIODE
    // =========================================================

    private String construirePeriode(LigneFraisDTO frais) {

        if (frais == null) {
            return "-";
        }

        if (frais.getTypeFraisFrequence() != null
                && (frais.getTypeFraisFrequence().equalsIgnoreCase("ANNUEL")
                || frais.getTypeFraisFrequence().equalsIgnoreCase("UNIQUE"))) {

            if (frais.getAnnee() != null) {
                return "Annuel " + frais.getAnnee();
            }
            return "Annuel";
        }

        Integer mois = frais.getMois();
        Integer annee = frais.getAnnee();

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

        return NOMS_MOIS[mois] + " " + annee;
    }


    // =========================================================
    // STATUT
    // =========================================================

    private String traduireStatut(String statut) {

        if (statut == null) {
            return "-";
        }

        return switch (statut.toUpperCase(Locale.FRANCE)) {
            case "PAYE" -> "PAYÉ";
            case "PARTIEL" -> "PARTIEL";
            case "NON_PAYE" -> "NON PAYÉ";
            default -> statut;
        };
    }


    // =========================================================
    // MONTANT
    // =========================================================

    private String formatMontant(Double montant) {

        if (montant == null) {
            return "0 FCFA";
        }

        return formatMontant(montant.doubleValue());
    }

    private String formatMontant(double montant) {

        return String.format(Locale.FRANCE, "%,.0f FCFA", montant).replace(",", " ");
    }


    // =========================================================
    // TEXTE
    // =========================================================

    private String valeur(String texte) {

        if (texte == null || texte.isBlank()) {
            return "-";
        }

        return texte;
    }

    /**
     * Adapte le texte à la largeur de la colonne pour éviter tout débordement.
     */
    private String adapterTexteColonne(String texte, float largeur) {

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

        return texte.substring(0, longueurMax - 1) + "…";
    }


    // =========================================================
    // NOUVELLE PAGE
    // =========================================================

    private PDPage nouvellePage(PDDocument document) {

        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        return page;
    }


    // =========================================================
    // HAUTEUR DISPONIBLE
    // =========================================================

    private float hauteurDisponible(PDPage page) {
        return page.getMediaBox().getHeight();
    }


    // =========================================================
    // PIED DE PAGE
    // =========================================================

    private void dessinerPiedsDePage(
            PDDocument document,
            PDType0Font regular
    ) throws IOException {

        int nombrePages = document.getNumberOfPages();
        String genereLe = "Document généré automatiquement le "
                + LocalDateTime.now().format(FORMAT_DATE_GENERATION) + ".";

        for (int i = 0; i < nombrePages; i++) {

            PDPage page = document.getPage(i);

            try (
                    PDPageContentStream cs = new PDPageContentStream(
                            document, page, PDPageContentStream.AppendMode.APPEND, true, true)
            ) {

                float largeur = page.getMediaBox().getWidth();
                float y = 30;

                ligneHorizontale(cs, MARGE, largeur - MARGE, 45, COULEUR_BORDURE, 0.75f);

                setCouleurTexte(cs, COULEUR_TEXTE_SECONDAIRE);

                cs.beginText();
                cs.setFont(regular, 7);
                cs.newLineAtOffset(MARGE, y);
                cs.showText(genereLe);
                cs.endText();

                String pageText = "Page " + (i + 1) + " / " + nombrePages;

                float largeurTexte = regular.getStringWidth(pageText) / 1000 * 7;

                cs.beginText();
                cs.setFont(regular, 7);
                cs.newLineAtOffset(largeur - MARGE - largeurTexte, y);
                cs.showText(pageText);
                cs.endText();
            }
        }
    }


    // =========================================================
    // RESULTATS
    // =========================================================

    private record ResultatTableau(PDPage page, float y) {
    }

    private record ResultatSection(PDPage page, float y) {
    }


    // =========================================================
    // MOIS
    // =========================================================

    private static final String[] NOMS_MOIS = {
            "",
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };
}