package com.saas.school.service;

import com.saas.school.entity.AffectationEnseignant;
import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Classe;
import com.saas.school.entity.CoefficientMatiere;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Enseignant;
import com.saas.school.entity.Eleve;
import com.saas.school.entity.Inscription;
import com.saas.school.entity.Matiere;
import com.saas.school.repository.AffectationEnseignantRepository;
import com.saas.school.repository.InscriptionRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReleveNoteService {

    private final AffectationEnseignantRepository affectationEnseignantRepository;
    private final InscriptionRepository inscriptionRepository;

    private static final float MARGE_GAUCHE = 40;
    private static final float MARGE_DROITE = 40;

    private static final float HAUTEUR_LIGNE = 23;

    /**
     * Génère un relevé de notes vierge pour une affectation enseignant.
     *
     * Le relevé contient :
     * - l'école
     * - l'année scolaire
     * - l'enseignant
     * - la classe
     * - la matière
     * - le coefficient
     * - la liste des élèves validés
     *
     * La colonne "Note composition" reste volontairement vide.
     */
    public byte[] genererRelevePdf(Long affectationId) {

        AffectationEnseignant affectation =
                affectationEnseignantRepository.findById(affectationId)
                        .orElseThrow(() ->
                                new RuntimeException("Affectation enseignant introuvable"));

        Enseignant enseignant = affectation.getEnseignant();
        Classe classe = affectation.getClasse();
        CoefficientMatiere coefficientMatiere =
                affectation.getCoefficientMatiere();

        if (coefficientMatiere == null) {
            throw new RuntimeException(
                    "Coefficient matière introuvable pour cette affectation"
            );
        }

        Matiere matiere = coefficientMatiere.getMatiere();
        AnneeScolaire anneeScolaire =
                coefficientMatiere.getAnneeScolaire();

        if (anneeScolaire == null) {
            throw new RuntimeException(
                    "Année scolaire introuvable pour cette affectation"
            );
        }

        /*
         * On récupère uniquement les élèves VALIDES
         * de cette classe et de cette année scolaire.
         */
        List<Inscription> inscriptions =
                inscriptionRepository.findElevesValidesPourReleve(
                        classe.getId(),
                        anneeScolaire.getId()
                );

        Ecole ecole = coefficientMatiere.getEcole();

        if (ecole == null && classe != null) {
            ecole = classe.getEcole();
        }

        try (PDDocument document = new PDDocument()) {

            // ============================================================
            // POLICES
            // ============================================================

            PDFont fontRegular;
            PDFont fontBold;

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

                fontRegular =
                        PDType0Font.load(document, regularStream);

                fontBold =
                        PDType0Font.load(document, boldStream);
            }

            // ============================================================
            // PREMIÈRE PAGE
            // ============================================================

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float largeurPage = PDRectangle.A4.getWidth();
            float hauteurPage = PDRectangle.A4.getHeight();

            float y = hauteurPage - 45;

            PDPageContentStream cs =
                    new PDPageContentStream(document, page);

            // ============================================================
            // EN-TÊTE
            // ============================================================

            y = ecrireCentre(
                    cs,
                    fontBold,
                    largeurPage,
                    y,
                    16,
                    nettoyerTexte(
                            ecole != null && ecole.getNom() != null
                                    ? ecole.getNom()
                                    : "ÉCOLE"
                    )
            );

            y -= 8;

            y = ecrireCentre(
                    cs,
                    fontBold,
                    largeurPage,
                    y,
                    14,
                    "RELEVÉ DE NOTES DE COMPOSITION"
            );

            y -= 6;

            cs.setStrokingColor(
                    0.70f,
                    0.70f,
                    0.70f
            );

            cs.moveTo(
                    MARGE_GAUCHE,
                    y
            );

            cs.lineTo(
                    largeurPage - MARGE_DROITE,
                    y
            );

            cs.stroke();

            y -= 25;

            // ============================================================
            // INFORMATIONS
            // ============================================================

            y = ecrireInformation(
                    cs,
                    fontRegular,
                    fontBold,
                    y,
                    "Enseignant",
                    nomEnseignant(enseignant)
            );

            y = ecrireInformation(
                    cs,
                    fontRegular,
                    fontBold,
                    y,
                    "Classe",
                    classe != null
                            ? classe.getNomComplet()
                            : null
            );

            y = ecrireInformation(
                    cs,
                    fontRegular,
                    fontBold,
                    y,
                    "Matière",
                    matiere != null
                            ? matiere.getNom()
                            : null
            );

            y = ecrireInformation(
                    cs,
                    fontRegular,
                    fontBold,
                    y,
                    "Coefficient",
                    coefficientMatiere.getCoefficient() != null
                            ? String.valueOf(
                            coefficientMatiere.getCoefficient()
                    )
                            : null
            );

            y = ecrireInformation(
                    cs,
                    fontRegular,
                    fontBold,
                    y,
                    "Année scolaire",
                    anneeScolaire.getNom()
            );

            y -= 15;

            // ============================================================
            // TABLEAU
            // ============================================================

            float x = MARGE_GAUCHE;

            float largeurTableau =
                    largeurPage
                            - MARGE_GAUCHE
                            - MARGE_DROITE;

            float largeurNumero = 35;
            float largeurNom = 140;
            float largeurPrenom = 140;
            float largeurNoteClasse = 90;

            float largeurNoteCompo =
                    largeurTableau
                            - largeurNumero
                            - largeurNom
                            - largeurPrenom
                            - largeurNoteClasse;
            // En-tête du tableau

            y = dessinerEnteteTableau(
                    cs,
                    fontBold,
                    x,
                    y,
                    largeurNumero,
                    largeurNom,
                    largeurPrenom,
                    largeurNoteClasse,
                    largeurNoteCompo
            );

            // ============================================================
            // ÉLÈVES
            // ============================================================

            int numero = 1;

            for (Inscription inscription : inscriptions) {

                /*
                 * Si le tableau arrive en bas de page,
                 * on ferme la page actuelle et on en crée une nouvelle.
                 */
                if (y - HAUTEUR_LIGNE < 65) {

                    cs.close();

                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);

                    cs = new PDPageContentStream(
                            document,
                            page
                    );

                    y = hauteurPage - 50;

                    y = ecrireCentre(
                            cs,
                            fontBold,
                            largeurPage,
                            y,
                            12,
                            "RELEVÉ DE NOTES DE COMPOSITION"
                    );

                    y -= 18;

                    y = dessinerEnteteTableau(
                            cs,
                            fontBold,
                            x,
                            y,
                            largeurNumero,
                            largeurNom,
                            largeurPrenom,
                            largeurNoteClasse,
                            largeurNoteCompo
                    );
                }

                Eleve eleve = inscription.getEleve();

                String nom =
                        eleve != null
                                ? eleve.getNom()
                                : "";

                String prenom =
                        eleve != null
                                ? eleve.getPrenom()
                                : "";

                dessinerLigneEleve(
                        cs,
                        fontRegular,
                        x,
                        y,
                        largeurNumero,
                        largeurNom,
                        largeurPrenom,
                        largeurNoteClasse,
                        largeurNoteCompo,
                        numero,
                        nom,
                        prenom
                );
                y -= HAUTEUR_LIGNE;

                numero++;
            }

            // ============================================================
            // BAS DU DOCUMENT
            // ============================================================

            if (y < 150) {

                cs.close();

                page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                cs = new PDPageContentStream(
                        document,
                        page
                );

                y = hauteurPage - 70;
            }

            y -= 25;

            // Nombre d'élèves

            cs.beginText();
            cs.setFont(fontBold, 10);
            cs.newLineAtOffset(
                    MARGE_GAUCHE,
                    y
            );

            cs.showText(
                    nettoyerTexte(
                            "Nombre d'élèves : "
                                    + inscriptions.size()
                    )
            );

            cs.endText();

            y -= 55;

            // ============================================================
            // SIGNATURES
            // ============================================================

            float largeurSignature =
                    (largeurTableau - 30) / 2;

            cs.setStrokingColor(
                    0.70f,
                    0.70f,
                    0.70f
            );

            // Enseignant

            cs.moveTo(
                    MARGE_GAUCHE,
                    y
            );

            cs.lineTo(
                    MARGE_GAUCHE + largeurSignature,
                    y
            );

            cs.stroke();

            // Secrétaire

            cs.moveTo(
                    MARGE_GAUCHE
                            + largeurSignature
                            + 30,
                    y
            );

            cs.lineTo(
                    largeurPage - MARGE_DROITE,
                    y
            );

            cs.stroke();

            y -= 15;

            cs.beginText();
            cs.setFont(fontRegular, 9);
            cs.newLineAtOffset(
                    MARGE_GAUCHE,
                    y
            );
            cs.showText(
                    "Signature de l'enseignant"
            );
            cs.endText();

            cs.beginText();
            cs.setFont(fontRegular, 9);
            cs.newLineAtOffset(
                    MARGE_GAUCHE
                            + largeurSignature
                            + 30,
                    y
            );
            cs.showText(
                    "Visa du secrétaire"
            );
            cs.endText();

            // ============================================================
            // PIED DE PAGE
            // ============================================================

            cs.beginText();
            cs.setFont(fontRegular, 8);
            cs.newLineAtOffset(
                    MARGE_GAUCHE,
                    30
            );

            cs.showText(
                    nettoyerTexte(
                            "Document destiné au remplissage manuel des notes de composition."
                    )
            );

            cs.endText();

            cs.close();

            // ============================================================
            // EXPORT
            // ============================================================

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            document.save(out);

            return out.toByteArray();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erreur lors de la génération du relevé de notes",
                    e
            );
        }
    }

    // ============================================================
    // TABLEAU
    // ============================================================

    private float dessinerEnteteTableau(
            PDPageContentStream cs,
            PDFont fontBold,
            float x,
            float y,
            float largeurNumero,
            float largeurNom,
            float largeurPrenom,
            float largeurNoteClasse,
            float largeurNoteCompo
    ) throws IOException {

        float hauteur = HAUTEUR_LIGNE;

        float total =
                largeurNumero
                        + largeurNom
                        + largeurPrenom
                        + largeurNoteClasse
                        + largeurNoteCompo;

        cs.setStrokingColor(0.35f, 0.35f, 0.35f);

        cs.addRect(
                x,
                y - hauteur,
                total,
                hauteur
        );

        cs.stroke();

        float x1 = x + largeurNumero;
        float x2 = x1 + largeurNom;
        float x3 = x2 + largeurPrenom;
        float x4 = x3 + largeurNoteClasse;

        cs.moveTo(x1, y);
        cs.lineTo(x1, y - hauteur);
        cs.stroke();

        cs.moveTo(x2, y);
        cs.lineTo(x2, y - hauteur);
        cs.stroke();

        cs.moveTo(x3, y);
        cs.lineTo(x3, y - hauteur);
        cs.stroke();

        cs.moveTo(x4, y);
        cs.lineTo(x4, y - hauteur);
        cs.stroke();

        ecrireCentreCellule(
                cs,
                fontBold,
                x,
                y,
                largeurNumero,
                "N°"
        );

        ecrireCentreCellule(
                cs,
                fontBold,
                x1,
                y,
                largeurNom,
                "NOM"
        );

        ecrireCentreCellule(
                cs,
                fontBold,
                x2,
                y,
                largeurPrenom,
                "PRÉNOM"
        );

        ecrireCentreCellule(
                cs,
                fontBold,
                x3,
                y,
                largeurNoteClasse,
                "NOTE CLASSE"
        );

        ecrireCentreCellule(
                cs,
                fontBold,
                x4,
                y,
                largeurNoteCompo,
                "NOTE COMPO"
        );

        return y - hauteur;
    }
    private void dessinerLigneEleve(
            PDPageContentStream cs,
            PDFont fontRegular,
            float x,
            float y,
            float largeurNumero,
            float largeurNom,
            float largeurPrenom,
            float largeurNoteClasse,
            float largeurNoteCompo,
            int numero,
            String nom,
            String prenom
    ) throws IOException {

        float hauteur = HAUTEUR_LIGNE;

        float total =
                largeurNumero
                        + largeurNom
                        + largeurPrenom
                        + largeurNoteClasse
                        + largeurNoteCompo;

        cs.setStrokingColor(0.65f, 0.65f, 0.65f);

        cs.addRect(
                x,
                y - hauteur,
                total,
                hauteur
        );

        cs.stroke();

        float x1 = x + largeurNumero;
        float x2 = x1 + largeurNom;
        float x3 = x2 + largeurPrenom;
        float x4 = x3 + largeurNoteClasse;

        cs.moveTo(x1, y);
        cs.lineTo(x1, y - hauteur);
        cs.stroke();

        cs.moveTo(x2, y);
        cs.lineTo(x2, y - hauteur);
        cs.stroke();

        cs.moveTo(x3, y);
        cs.lineTo(x3, y - hauteur);
        cs.stroke();

        cs.moveTo(x4, y);
        cs.lineTo(x4, y - hauteur);
        cs.stroke();

        // N°
        ecrireCentreCellule(
                cs,
                fontRegular,
                x,
                y,
                largeurNumero,
                String.valueOf(numero)
        );

        // Nom
        ecrireCellule(
                cs,
                fontRegular,
                x1,
                y,
                largeurNom,
                nom
        );

        // Prénom
        ecrireCellule(
                cs,
                fontRegular,
                x2,
                y,
                largeurPrenom,
                prenom
        );

        /*
         * NOTE CLASSE : vide
         * NOTE COMPO  : vide
         *
         * Les deux notes seront remplies manuellement
         * par l'enseignant.
         */
    }
    // ============================================================
    // INFORMATIONS
    // ============================================================

    private float ecrireInformation(
            PDPageContentStream cs,
            PDFont fontRegular,
            PDFont fontBold,
            float y,
            String label,
            String valeur
    ) throws IOException {

        if (!notBlank(valeur)) {
            return y;
        }

        cs.beginText();
        cs.setFont(fontBold, 10);
        cs.newLineAtOffset(
                MARGE_GAUCHE,
                y
        );

        cs.showText(
                nettoyerTexte(label + " :")
        );

        cs.endText();

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(
                MARGE_GAUCHE + 120,
                y
        );

        cs.showText(
                nettoyerTexte(valeur)
        );

        cs.endText();

        return y - 18;
    }

    // ============================================================
    // TEXTE
    // ============================================================

    private float ecrireCentre(
            PDPageContentStream cs,
            PDFont font,
            float largeurPage,
            float y,
            float taille,
            String texte
    ) throws IOException {

        texte = nettoyerTexte(texte);

        float largeur =
                font.getStringWidth(texte)
                        / 1000
                        * taille;

        float x =
                (largeurPage - largeur) / 2;

        cs.beginText();
        cs.setFont(font, taille);
        cs.newLineAtOffset(x, y);
        cs.showText(texte);
        cs.endText();

        return y - taille - 5;
    }

    private void ecrireCentreCellule(
            PDPageContentStream cs,
            PDFont font,
            float x,
            float y,
            float largeur,
            String texte
    ) throws IOException {

        texte = nettoyerTexte(texte);

        float taille = 8;

        float largeurTexte =
                font.getStringWidth(texte)
                        / 1000
                        * taille;

        float positionX =
                x + (largeur - largeurTexte) / 2;

        float positionY =
                y - 15;
        cs.beginText();
        cs.setFont(font, taille);
        cs.newLineAtOffset(
                positionX,
                positionY
        );
        cs.showText(texte);
        cs.endText();
    }

    private void ecrireCellule(
            PDPageContentStream cs,
            PDFont font,
            float x,
            float y,
            float largeur,
            String texte
    ) throws IOException {

        texte = nettoyerTexte(texte);

        if (texte == null || texte.isBlank()) {
            return;
        }

        float taille = 9;

        cs.beginText();
        cs.setFont(font, taille);
        cs.newLineAtOffset(
                x + 5,
                y - 15
        );
        cs.showText(texte);
        cs.endText();
    }

    // ============================================================
    // ENSEIGNANT
    // ============================================================

    private String nomEnseignant(Enseignant enseignant) {

        if (enseignant == null) {
            return null;
        }

        String prenom =
                enseignant.getPrenom() != null
                        ? enseignant.getPrenom()
                        : "";

        String nom =
                enseignant.getNom() != null
                        ? enseignant.getNom()
                        : "";

        return (prenom + " " + nom).trim();
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private boolean notBlank(String valeur) {
        return valeur != null
                && !valeur.isBlank();
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
}