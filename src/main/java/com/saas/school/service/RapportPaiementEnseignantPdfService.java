package com.saas.school.service;

import com.saas.school.dto.RapportPaiementEnseignantDTO;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Enseignant;
import com.saas.school.repository.EnseignantRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RapportPaiementEnseignantPdfService {

    private final PaiementEnseignantService paiementEnseignantService;
    private final EnseignantRepository enseignantRepo;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private static final float MARGE_GAUCHE = 45;
    private static final float MARGE_BAS = 50;

    // État de pagination mutable pendant la génération
    private PDDocument document;
    private PDPageContentStream cs;
    private float y;
    private float largeurPage;
    private PDFont fontRegular;
    private PDFont fontBold;

    public byte[] genererRapportPdf(Long enseignantId, Long anneeId) {

        RapportPaiementEnseignantDTO rapport =
                paiementEnseignantService.rapportEnseignant(enseignantId, anneeId);

        Enseignant enseignant = enseignantRepo.findById(enseignantId)
                .orElseThrow(() -> new RuntimeException("Enseignant introuvable"));

        Ecole ecole = enseignant.getEcole();

        document = new PDDocument();

        try {
            // ===== POLICES UNICODE =====
            try (InputStream regularStream = new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream();
                 InputStream boldStream = new ClassPathResource("fonts/DejaVuSans-Bold.ttf").getInputStream()) {

                fontRegular = PDType0Font.load(document, regularStream);
                fontBold = PDType0Font.load(document, boldStream);
            }

            nouvellePage();

            // ===== EN-TÊTE ÉCOLE =====
            ecrireTitre(ecole != null && ecole.getNom() != null ? nettoyerTexte(ecole.getNom()) : "École", 16);
            y -= 26;

            ecrireTitre("RAPPORT DE PAIEMENTS ENSEIGNANT", 14);
            y -= 15;

            ligneHorizontale();
            y -= 25;

            // ===== IDENTITÉ ENSEIGNANT =====
            String nomComplet = (rapport.getEnseignantPrenom() != null ? rapport.getEnseignantPrenom() : "")
                    + " " + (rapport.getEnseignantNom() != null ? rapport.getEnseignantNom() : "");

            y = ecrireLigne(MARGE_GAUCHE, y, "Enseignant", nomComplet.trim());

            if (rapport.getMatricule() != null) {
                y = ecrireLigne(MARGE_GAUCHE, y, "Matricule", rapport.getMatricule());
            }

            y = ecrireLigne(MARGE_GAUCHE, y, "Type de contrat", libelleContrat(rapport.getTypeContrat()));

            y -= 15;

            // ===== RÉSUMÉ GLOBAL =====
            ecrireSousTitre("Résumé de l'année scolaire");
            y -= 22;

            y = ecrireLigne(MARGE_GAUCHE, y, "Total heures", rapport.getTotalHeures() + " h");
            y = ecrireLigne(MARGE_GAUCHE, y, "Total montant heures", formatMontant(rapport.getTotalMontantHeures()));
            y = ecrireLigne(MARGE_GAUCHE, y, "Total salaire de base", formatMontant(rapport.getTotalSalaireBase()));
            y = ecrireLigne(MARGE_GAUCHE, y, "Total payé", formatMontant(rapport.getTotalPaye()));
            y = ecrireLigne(MARGE_GAUCHE, y, "Total en attente", formatMontant(rapport.getTotalEnAttente()));

            y -= 15;

            // ===== TOTAL GÉNÉRAL (encadré) =====
            encadrerMontant("Total général", rapport.getTotalMontant());

// ===== TABLEAU DES PAIEMENTS =====
            ecrireSousTitre("Détail des paiements");

            ecrireEnteteTableau();
            if (rapport.getPaiements() == null || rapport.getPaiements().isEmpty()) {

                verifierSautDePage(20);
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(MARGE_GAUCHE, y);
                cs.showText("Aucun paiement enregistré pour cette année scolaire.");
                cs.endText();
                y -= 20;

            } else {

                for (RapportPaiementEnseignantDTO.PaiementLigneDTO ligne : rapport.getPaiements()) {
                    ecrireLigneTableau(ligne);
                }
            }

            // ===== PIED DE PAGE =====
            verifierSautDePage(30);
            cs.beginText();
            cs.setFont(fontRegular, 9);
            cs.newLineAtOffset(MARGE_GAUCHE, MARGE_BAS - 10);
            cs.showText("Document généré automatiquement — rapport annuel des paiements enseignant.");
            cs.endText();

            cs.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du rapport PDF", e);
        } finally {
            try {
                document.close();
            } catch (IOException ignored) {
            }
        }
    }

    // ============================================================
    // PAGINATION
    // ============================================================

    private void nouvellePage() throws IOException {

        if (cs != null) {
            cs.close();
        }

        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        largeurPage = PDRectangle.A4.getWidth();
        y = PDRectangle.A4.getHeight() - 50;

        cs = new PDPageContentStream(document, page);
    }

    /**
     * Passe à une nouvelle page si l'espace restant est insuffisant
     * pour la hauteur demandée.
     */
    private void verifierSautDePage(float hauteurNecessaire) throws IOException {
        if (y - hauteurNecessaire < MARGE_BAS) {
            nouvellePage();
        }
    }

    // ============================================================
    // HELPERS D'ÉCRITURE
    // ============================================================

    private void ecrireTitre(String texte, int taille) throws IOException {
        cs.beginText();
        cs.setFont(fontBold, taille);
        cs.newLineAtOffset(MARGE_GAUCHE, y);
        cs.showText(nettoyerTexte(texte));
        cs.endText();
    }

    private void ecrireSousTitre(String texte) throws IOException {

        verifierSautDePage(30);

        cs.beginText();
        cs.setFont(fontBold, 12);
        cs.newLineAtOffset(MARGE_GAUCHE, y);
        cs.showText(nettoyerTexte(texte));
        cs.endText();

        y -= 18;
    }

    private void ligneHorizontale() throws IOException {
        cs.setStrokingColor(0.78f, 0.78f, 0.78f);
        cs.moveTo(MARGE_GAUCHE, y);
        cs.lineTo(largeurPage - MARGE_GAUCHE, y);
        cs.stroke();
    }

    private float ecrireLigne(float x, float yActuel, String label, String valeur) throws IOException {

        verifierSautDePage(20);

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(nettoyerTexte(label) + " :");
        cs.endText();

        cs.beginText();
        cs.setFont(fontBold, 10);
        cs.newLineAtOffset(x + 160, y);
        cs.showText(nettoyerTexte(valeur != null && !valeur.isBlank() ? valeur : "-"));
        cs.endText();

        y -= 18;
        return y;
    }

    private void encadrerMontant(String label, Double montant) throws IOException {

        final float HAUTEUR_BLOC = 45;
        final float ESPACE_APRES = 20;

        // Vérifie que le bloc complet + l'espace après peuvent tenir
        verifierSautDePage(HAUTEUR_BLOC + ESPACE_APRES);

        cs.setStrokingColor(0.86f, 0.86f, 0.86f);

        cs.addRect(
                MARGE_GAUCHE,
                y - HAUTEUR_BLOC + 5,
                largeurPage - 2 * MARGE_GAUCHE,
                HAUTEUR_BLOC
        );

        cs.stroke();

        // Libellé
        cs.beginText();
        cs.setFont(fontRegular, 11);
        cs.newLineAtOffset(MARGE_GAUCHE + 15, y - 12);
        cs.showText(nettoyerTexte(label));
        cs.endText();

        // Montant
        cs.beginText();
        cs.setFont(fontBold, 18);
        cs.newLineAtOffset(MARGE_GAUCHE + 15, y - 32);
        cs.showText(formatMontant(montant));
        cs.endText();

        // IMPORTANT :
        // On descend le curseur après le bloc
        y -= HAUTEUR_BLOC + ESPACE_APRES;
    }
    // Colonnes du tableau (positions X relatives à MARGE_GAUCHE)
    private static final float COL_PERIODE = 0;
    private static final float COL_HEURES = 140;
    private static final float COL_TAUX = 180;
    private static final float COL_SALAIRE = 225;
    private static final float COL_MONTANT_H = 300;
    private static final float COL_MONTANT = 370;
    private static final float COL_STATUT = 440;

    private void ecrireEnteteTableau() throws IOException {

        verifierSautDePage(25);

        cs.beginText();
        cs.setFont(fontBold, 8.5f);
        cs.newLineAtOffset(MARGE_GAUCHE + COL_PERIODE, y);
        cs.showText("PERIODE");
        cs.endText();

        cs.beginText();
        cs.setFont(fontBold, 8.5f);
        cs.newLineAtOffset(MARGE_GAUCHE + COL_HEURES, y);
        cs.showText("HEURES");
        cs.endText();

        cs.beginText();
        cs.setFont(fontBold, 8.5f);
        cs.newLineAtOffset(MARGE_GAUCHE + COL_TAUX, y);
        cs.showText("TAUX");
        cs.endText();

        cs.beginText();
        cs.setFont(fontBold, 8.5f);
        cs.newLineAtOffset(MARGE_GAUCHE + COL_SALAIRE, y);
        cs.showText("SALAIRE BASE");
        cs.endText();

        cs.beginText();
        cs.setFont(fontBold, 8.5f);
        cs.newLineAtOffset(MARGE_GAUCHE + COL_MONTANT_H, y);
        cs.showText("MONT. HEURES");
        cs.endText();

        cs.beginText();
        cs.setFont(fontBold, 8.5f);
        cs.newLineAtOffset(MARGE_GAUCHE + COL_MONTANT, y);
        cs.showText("MONTANT");
        cs.endText();

        cs.beginText();
        cs.setFont(fontBold, 8.5f);
        cs.newLineAtOffset(MARGE_GAUCHE + COL_STATUT, y);
        cs.showText("STATUT");
        cs.endText();

        y -= 6;
        ligneHorizontale();
        y -= 16;
    }

    private void ecrireLigneTableau(RapportPaiementEnseignantDTO.PaiementLigneDTO ligne) throws IOException {

        verifierSautDePage(20);

        String periode = ligne.getPeriodeDebut() != null && ligne.getPeriodeFin() != null
                ? ligne.getPeriodeDebut().format(DATE_FORMAT) + " - " + ligne.getPeriodeFin().format(DATE_FORMAT)
                : "-";

        ecrireCellule(COL_PERIODE, periode, 9);
        ecrireCellule(COL_HEURES, ligne.getTotalHeures() + "h", 9);
        ecrireCellule(COL_TAUX, formatMontantCourt(ligne.getTauxHoraire()), 9);
        ecrireCellule(COL_SALAIRE, formatMontantCourt(ligne.getSalaireBase()), 9);
        ecrireCellule(COL_MONTANT_H, formatMontantCourt(ligne.getMontantHeures()), 9);
        ecrireCellule(COL_MONTANT, formatMontantCourt(ligne.getMontant()), 9);
        ecrireCellule(COL_STATUT, libelleStatut(ligne.getStatut()), 9);

        y -= 18;
    }

    private void ecrireCellule(float colX, String texte, int taille) throws IOException {
        cs.beginText();
        cs.setFont(fontRegular, taille);
        cs.newLineAtOffset(MARGE_GAUCHE + colX, y);
        cs.showText(nettoyerTexte(texte != null ? texte : "-"));
        cs.endText();
    }

    // ============================================================
    // FORMATAGE
    // ============================================================

    private String formatMontant(Double montant) {
        if (montant == null) return "-";
        return String.format(Locale.FRANCE, "%,.0f FCFA", montant)
                .replace('\u202F', ' ')
                .replace('\u00A0', ' ')
                .replace(",", " ");
    }

    private String formatMontantCourt(Double montant) {
        if (montant == null) return "-";
        return String.format(Locale.FRANCE, "%,.0f", montant)
                .replace('\u202F', ' ')
                .replace('\u00A0', ' ')
                .replace(",", " ");
    }

    private String nettoyerTexte(String texte) {
        if (texte == null) return "";
        return texte
                .replace('\u202F', ' ')
                .replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u2009', ' ');
    }

    private String libelleStatut(String statut) {
        if (statut == null) return "-";
        return switch (statut) {
            case "PAYE" -> "Payé";
            case "EN_ATTENTE" -> "En attente";
            default -> statut;
        };
    }

    private String libelleContrat(String contrat) {
        if (contrat == null) return "-";
        return switch (contrat) {
            case "CDI" -> "CDI";
            case "CDD" -> "CDD";
            case "VACATAIRE" -> "Vacataire";
            case "STAGIAIRE" -> "Stagiaire";
            default -> contrat;
        };
    }
}