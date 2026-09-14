package com.saas.school.service;

import com.saas.school.entity.Ecole;
import com.saas.school.repository.EcoleRepository;
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

@Service
@RequiredArgsConstructor
public class FormulaireInscriptionService {

    private final EcoleRepository ecoleRepository;

    private static final float MARGE_GAUCHE = 50;
    private static final float LARGEUR_LIGNE_LONGUE = 260;
    private static final float LARGEUR_LIGNE_COURTE = 140;

    private PDFont fontRegular;
    private PDFont fontBold;
    private PDPageContentStream cs;
    private float y;
    private float largeurPage;

    /**
     * Génère un formulaire VIERGE (aucune donnée pré-remplie), destiné à
     * être imprimé et rempli à la main par le parent au moment de
     * l'inscription — contrairement à la fiche de renseignement, qui
     * imprime des données déjà saisies dans le système.
     */
    public byte[] genererFormulairePdf(Long ecoleId) {

        Ecole ecole = ecoleRepository.findById(ecoleId)
                .orElseThrow(() -> new RuntimeException("École introuvable"));

        try (PDDocument document = new PDDocument()) {

            try (InputStream regularStream = new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream();
                 InputStream boldStream = new ClassPathResource("fonts/DejaVuSans-Bold.ttf").getInputStream()) {

                fontRegular = PDType0Font.load(document, regularStream);
                fontBold = PDType0Font.load(document, boldStream);
            }

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            largeurPage = PDRectangle.A4.getWidth();
            y = PDRectangle.A4.getHeight() - 55;

            cs = new PDPageContentStream(document, page);

            // ===== EN-TÊTE ÉCOLE =====
            ecrireTitre(nettoyerTexte(ecole.getNom() != null ? ecole.getNom() : "École"), 16);
            y -= 26;

            ecrireTitre("FORMULAIRE D'INSCRIPTION", 14);
            y -= 6;

            cs.beginText();
            cs.setFont(fontRegular, 9);
            cs.newLineAtOffset(MARGE_GAUCHE, y);
            cs.showText("À remplir par le parent ou tuteur légal");
            cs.endText();

            y -= 15;
            ligneHorizontale();
            y -= 25;

            // ===== IDENTITÉ DE L'ÉLÈVE =====
            ecrireSection("Identité de l'élève");

            champSurLigne("Nom", LARGEUR_LIGNE_LONGUE, "Prénom", LARGEUR_LIGNE_LONGUE);
            champSurLigne("Date de naissance", LARGEUR_LIGNE_COURTE, "Lieu de naissance", LARGEUR_LIGNE_LONGUE);
            champChoix("Sexe", "☐ Masculin      ☐ Féminin");
            champSurLigne("Nationalité", LARGEUR_LIGNE_LONGUE, "N° extrait de naissance", LARGEUR_LIGNE_LONGUE);
            champSimple("Groupe sanguin", LARGEUR_LIGNE_COURTE);
            champLargeur("Allergies / maladies particulières", largeurPage - 2 * MARGE_GAUCHE - 160);

            y -= 10;

            // ===== COORDONNÉES =====
            ecrireSection("Coordonnées de l'élève");

            champLargeur("Adresse", largeurPage - 2 * MARGE_GAUCHE - 160);
            champSurLigne("Téléphone", LARGEUR_LIGNE_COURTE, "Email", LARGEUR_LIGNE_LONGUE);

            y -= 10;

            // ===== PARENT / TUTEUR =====
            ecrireSection("Parent / Tuteur légal");

            champSurLigne("Nom", LARGEUR_LIGNE_LONGUE, "Prénom", LARGEUR_LIGNE_LONGUE);
            champSurLigne("Lien de parenté", LARGEUR_LIGNE_COURTE, "Profession", LARGEUR_LIGNE_LONGUE);
            champSurLigne("Téléphone", LARGEUR_LIGNE_COURTE, "Email", LARGEUR_LIGNE_LONGUE);
            champLargeur("Adresse", largeurPage - 2 * MARGE_GAUCHE - 160);

            y -= 10;

            // ===== CONTACT D'URGENCE =====
            ecrireSection("Contact d'urgence (si différent du tuteur)");

            champSurLigne("Nom complet", LARGEUR_LIGNE_LONGUE, "Téléphone", LARGEUR_LIGNE_LONGUE);

            y -= 10;

            // ===== SCOLARITÉ SOUHAITÉE =====
            ecrireSection("Scolarité souhaitée");

            champSurLigne("Classe demandée", LARGEUR_LIGNE_LONGUE, "Année scolaire", LARGEUR_LIGNE_LONGUE);
            champSimple("École de provenance (si transfert)", LARGEUR_LIGNE_LONGUE);

            y -= 15;

            // ===== DOCUMENTS FOURNIS =====
            ecrireSection("Documents fournis");

            caseACocher("Extrait d'acte de naissance");
            caseACocher("Certificat médical");
            caseACocher("Photos d'identité");
            caseACocher("Bulletin / carnet de notes de l'année précédente (si transfert)");
            caseACocher("Certificat de radiation (si transfert)");

            y -= 25;

            // ===== SIGNATURE =====
            float largeurSignature = (largeurPage - 2 * MARGE_GAUCHE - 20) / 2;

            cs.setStrokingColor(0.7f, 0.7f, 0.7f);
            cs.moveTo(MARGE_GAUCHE, y);
            cs.lineTo(MARGE_GAUCHE + largeurSignature, y);
            cs.stroke();

            cs.moveTo(MARGE_GAUCHE + largeurSignature + 20, y);
            cs.lineTo(MARGE_GAUCHE + 2 * largeurSignature + 20, y);
            cs.stroke();

            y -= 15;

            cs.beginText();
            cs.setFont(fontRegular, 9);
            cs.newLineAtOffset(MARGE_GAUCHE, y);
            cs.showText("Date et signature du parent / tuteur");
            cs.endText();

            cs.beginText();
            cs.setFont(fontRegular, 9);
            cs.newLineAtOffset(MARGE_GAUCHE + largeurSignature + 20, y);
            cs.showText("Cadre réservé à l'administration");
            cs.endText();

            cs.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du formulaire d'inscription", e);
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private void ecrireTitre(String texte, int taille) throws IOException {
        cs.beginText();
        cs.setFont(fontBold, taille);
        cs.newLineAtOffset(MARGE_GAUCHE, y);
        cs.showText(texte);
        cs.endText();
    }

    private void ecrireSection(String titre) throws IOException {
        cs.beginText();
        cs.setFont(fontBold, 11);
        cs.newLineAtOffset(MARGE_GAUCHE, y);
        cs.showText(nettoyerTexte(titre));
        cs.endText();
        y -= 22;
    }

    private void ligneHorizontale() throws IOException {
        cs.setStrokingColor(0.78f, 0.78f, 0.78f);
        cs.moveTo(MARGE_GAUCHE, y);
        cs.lineTo(largeurPage - MARGE_GAUCHE, y);
        cs.stroke();
    }

    /**
     * Deux champs "label + ligne à remplir" côte à côte sur la même rangée.
     */
    private void champSurLigne(String label1, float largeur1, String label2, float largeur2) throws IOException {

        float x2 = MARGE_GAUCHE + largeur1 + 30;

        ecrireLabel(MARGE_GAUCHE, label1);
        ecrireLabel(x2, label2);

        y -= 14;

        dessinerLigneVide(MARGE_GAUCHE, largeur1);
        dessinerLigneVide(x2, largeur2);

        y -= 24;
    }

    /**
     * Un seul champ label + ligne à remplir.
     */
    private void champSimple(String label, float largeur) throws IOException {

        ecrireLabel(MARGE_GAUCHE, label);
        y -= 14;
        dessinerLigneVide(MARGE_GAUCHE, largeur);
        y -= 24;
    }

    /**
     * Champ à choix multiple (ex: sexe) — pas de ligne à remplir,
     * juste les options à cocher affichées sur la même ligne.
     */
    private void champChoix(String label, String optionsTexte) throws IOException {

        ecrireLabel(MARGE_GAUCHE, label);
        y -= 14;

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGE_GAUCHE, y);
        cs.showText(optionsTexte);
        cs.endText();

        y -= 24;
    }

    /**
     * Champ label + ligne pleine largeur (adresse, allergies, etc.).
     */
    private void champLargeur(String label, float largeur) throws IOException {

        ecrireLabel(MARGE_GAUCHE, label);
        y -= 14;
        dessinerLigneVide(MARGE_GAUCHE, largeur);
        y -= 24;
    }

    private void ecrireLabel(float x, String label) throws IOException {
        cs.beginText();
        cs.setFont(fontRegular, 9.5f);
        cs.newLineAtOffset(x, y);
        cs.showText(nettoyerTexte(label));
        cs.endText();
    }

    private void dessinerLigneVide(float x, float largeur) throws IOException {
        cs.setStrokingColor(0.6f, 0.6f, 0.6f);
        cs.moveTo(x, y);
        cs.lineTo(x + largeur, y);
        cs.stroke();
    }

    private void caseACocher(String libelle) throws IOException {

        cs.setStrokingColor(0.5f, 0.5f, 0.5f);
        cs.addRect(MARGE_GAUCHE, y - 8, 10, 10);
        cs.stroke();

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(MARGE_GAUCHE + 16, y - 6);
        cs.showText(nettoyerTexte(libelle));
        cs.endText();

        y -= 20;
    }

    private String nettoyerTexte(String texte) {
        if (texte == null) return "";
        return texte
                .replace('\u202F', ' ')
                .replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u2009', ' ');
    }
}