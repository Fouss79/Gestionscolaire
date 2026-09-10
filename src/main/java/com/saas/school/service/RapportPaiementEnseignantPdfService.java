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
    private static final float MARGE_DROITE = 45;
    private static final float MARGE_HAUT = 50;
    private static final float MARGE_BAS = 50;

    // ============================================================
    // GÉNÉRATION DU PDF
    // ============================================================

    public byte[] genererRapportPdf(Long enseignantId, Long anneeId) {

        RapportPaiementEnseignantDTO rapport =
                paiementEnseignantService.rapportEnseignant(
                        enseignantId,
                        anneeId
                );

        Enseignant enseignant = enseignantRepo.findById(enseignantId)
                .orElseThrow(() ->
                        new RuntimeException("Enseignant introuvable"));

        Ecole ecole = enseignant.getEcole();

        /*
         * IMPORTANT :
         * Toutes les variables PDFBox sont locales à cette génération.
         * Rien n'est partagé entre deux téléchargements.
         */
        try (PDDocument document = new PDDocument()) {

            PDFont fontRegular;
            PDFont fontBold;

            // ====================================================
            // POLICES
            // ====================================================

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

            // ====================================================
            // ÉTAT LOCAL DE LA GÉNÉRATION
            // ====================================================

            PdfContext ctx = new PdfContext(
                    document,
                    fontRegular,
                    fontBold
            );

            // ====================================================
            // PREMIÈRE PAGE
            // ====================================================

            nouvellePage(ctx);

            // ====================================================
            // EN-TÊTE
            // ====================================================

            String nomEcole =
                    ecole != null && ecole.getNom() != null
                            ? nettoyerTexte(ecole.getNom())
                            : "École";

            ecrireTitre(ctx, nomEcole, 16);

            ctx.y -= 26;

            ecrireTitre(
                    ctx,
                    "RAPPORT DE PAIEMENTS ENSEIGNANT",
                    14
            );

            ctx.y -= 15;

            ligneHorizontale(ctx);

            ctx.y -= 25;

            // ====================================================
            // IDENTITÉ ENSEIGNANT
            // ====================================================

            String nomComplet =
                    (rapport.getEnseignantPrenom() != null
                            ? rapport.getEnseignantPrenom()
                            : "")
                            + " "
                            + (rapport.getEnseignantNom() != null
                            ? rapport.getEnseignantNom()
                            : "");

            ctx.y = ecrireLigne(
                    ctx,
                    MARGE_GAUCHE,
                    ctx.y,
                    "Enseignant",
                    nomComplet.trim()
            );

            if (rapport.getMatricule() != null) {

                ctx.y = ecrireLigne(
                        ctx,
                        MARGE_GAUCHE,
                        ctx.y,
                        "Matricule",
                        rapport.getMatricule()
                );
            }

            ctx.y = ecrireLigne(
                    ctx,
                    MARGE_GAUCHE,
                    ctx.y,
                    "Type de contrat",
                    libelleContrat(
                            rapport.getTypeContrat()
                    )
            );

            ctx.y -= 15;

            // ====================================================
            // RÉSUMÉ
            // ====================================================

            ecrireSousTitre(
                    ctx,
                    "Résumé de l'année scolaire"
            );

            ctx.y -= 4;

            ctx.y = ecrireLigne(
                    ctx,
                    MARGE_GAUCHE,
                    ctx.y,
                    "Total heures",
                    rapport.getTotalHeures() + " h"
            );

            ctx.y = ecrireLigne(
                    ctx,
                    MARGE_GAUCHE,
                    ctx.y,
                    "Total montant heures",
                    formatMontant(
                            rapport.getTotalMontantHeures()
                    )
            );

            ctx.y = ecrireLigne(
                    ctx,
                    MARGE_GAUCHE,
                    ctx.y,
                    "Total salaire de base",
                    formatMontant(
                            rapport.getTotalSalaireBase()
                    )
            );

            ctx.y = ecrireLigne(
                    ctx,
                    MARGE_GAUCHE,
                    ctx.y,
                    "Total payé",
                    formatMontant(
                            rapport.getTotalPaye()
                    )
            );

            ctx.y = ecrireLigne(
                    ctx,
                    MARGE_GAUCHE,
                    ctx.y,
                    "Total en attente",
                    formatMontant(
                            rapport.getTotalEnAttente()
                    )
            );

            ctx.y -= 15;

            // ====================================================
            // TOTAL GÉNÉRAL
            // ====================================================

            encadrerMontant(
                    ctx,
                    "Total général",
                    rapport.getTotalMontant()
            );

            // ====================================================
            // DÉTAIL DES PAIEMENTS
            // ====================================================

            ecrireSousTitre(
                    ctx,
                    "Détail des paiements"
            );

            ctx.y -= 5;

            ecrireEnteteTableau(ctx);

            // ====================================================
            // LIGNES
            // ====================================================

            if (
                    rapport.getPaiements() == null
                            || rapport.getPaiements().isEmpty()
            ) {

                verifierSautDePage(ctx, 20);

                ecrireTexte(
                        ctx,
                        "Aucun paiement enregistré pour cette année scolaire.",
                        MARGE_GAUCHE,
                        ctx.y,
                        fontRegular,
                        10
                );

                ctx.y -= 20;

            } else {

                for (
                        RapportPaiementEnseignantDTO.PaiementLigneDTO ligne
                        : rapport.getPaiements()
                ) {

                    ecrireLigneTableau(ctx, ligne);
                }
            }

            // ====================================================
            // PIED DE PAGE
            // ====================================================

            ajouterPiedDePage(ctx);

            // Fermer le content stream courant
            fermerPage(ctx);

            // ====================================================
            // SAUVEGARDE
            // ====================================================

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            document.save(out);

            return out.toByteArray();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erreur lors de la génération du rapport PDF",
                    e
            );
        }
    }

    // ============================================================
    // CONTEXTE PDF
    // ============================================================

    private static class PdfContext {

        private final PDDocument document;

        private final PDFont fontRegular;

        private final PDFont fontBold;

        private PDPageContentStream cs;

        private float y;

        private float largeurPage;

        private PdfContext(
                PDDocument document,
                PDFont fontRegular,
                PDFont fontBold
        ) {

            this.document = document;
            this.fontRegular = fontRegular;
            this.fontBold = fontBold;
        }
    }

    // ============================================================
    // PAGINATION
    // ============================================================

    private void nouvellePage(PdfContext ctx)
            throws IOException {

        fermerPage(ctx);

        PDPage page =
                new PDPage(PDRectangle.A4);

        ctx.document.addPage(page);

        ctx.largeurPage =
                PDRectangle.A4.getWidth();

        ctx.y =
                PDRectangle.A4.getHeight()
                        - MARGE_HAUT;

        ctx.cs =
                new PDPageContentStream(
                        ctx.document,
                        page
                );
    }

    private void fermerPage(PdfContext ctx)
            throws IOException {

        if (ctx.cs != null) {

            ctx.cs.close();

            ctx.cs = null;
        }
    }

    private void verifierSautDePage(
            PdfContext ctx,
            float hauteurNecessaire
    ) throws IOException {

        if (
                ctx.y - hauteurNecessaire
                        < MARGE_BAS
        ) {

            nouvellePage(ctx);
        }
    }

    // ============================================================
    // TITRES
    // ============================================================

    private void ecrireTitre(
            PdfContext ctx,
            String texte,
            int taille
    ) throws IOException {

        verifierSautDePage(ctx, 25);

        ecrireTexte(
                ctx,
                texte,
                MARGE_GAUCHE,
                ctx.y,
                ctx.fontBold,
                taille
        );
    }

    private void ecrireSousTitre(
            PdfContext ctx,
            String texte
    ) throws IOException {

        verifierSautDePage(ctx, 30);

        ecrireTexte(
                ctx,
                texte,
                MARGE_GAUCHE,
                ctx.y,
                ctx.fontBold,
                12
        );

        ctx.y -= 18;
    }

    // ============================================================
    // TEXTE
    // ============================================================

    private void ecrireTexte(
            PdfContext ctx,
            String texte,
            float x,
            float y,
            PDFont font,
            float taille
    ) throws IOException {

        ctx.cs.beginText();

        ctx.cs.setFont(
                font,
                taille
        );

        ctx.cs.newLineAtOffset(
                x,
                y
        );

        ctx.cs.showText(
                nettoyerTexte(
                        texte != null
                                ? texte
                                : ""
                )
        );

        ctx.cs.endText();
    }

    // ============================================================
    // LIGNE HORIZONTALE
    // ============================================================

    private void ligneHorizontale(
            PdfContext ctx
    ) throws IOException {

        ctx.cs.setStrokingColor(
                0.78f,
                0.78f,
                0.78f
        );

        ctx.cs.moveTo(
                MARGE_GAUCHE,
                ctx.y
        );

        ctx.cs.lineTo(
                ctx.largeurPage
                        - MARGE_DROITE,
                ctx.y
        );

        ctx.cs.stroke();
    }

    // ============================================================
    // LIGNE INFORMATIONS
    // ============================================================

    private float ecrireLigne(
            PdfContext ctx,
            float x,
            float yActuel,
            String label,
            String valeur
    ) throws IOException {

        verifierSautDePage(ctx, 20);

        ecrireTexte(
                ctx,
                label + " :",
                x,
                ctx.y,
                ctx.fontRegular,
                10
        );

        ecrireTexte(
                ctx,
                valeur != null && !valeur.isBlank()
                        ? valeur
                        : "-",
                x + 160,
                ctx.y,
                ctx.fontBold,
                10
        );

        return ctx.y - 18;
    }

    // ============================================================
    // TOTAL GÉNÉRAL
    // ============================================================

    private void encadrerMontant(
            PdfContext ctx,
            String label,
            Double montant
    ) throws IOException {

        final float HAUTEUR_BLOC = 45;

        final float ESPACE_APRES = 20;

        verifierSautDePage(
                ctx,
                HAUTEUR_BLOC
                        + ESPACE_APRES
        );

        ctx.cs.setStrokingColor(
                0.86f,
                0.86f,
                0.86f
        );

        ctx.cs.addRect(
                MARGE_GAUCHE,
                ctx.y - HAUTEUR_BLOC + 5,
                ctx.largeurPage
                        - MARGE_GAUCHE
                        - MARGE_DROITE,
                HAUTEUR_BLOC
        );

        ctx.cs.stroke();

        ecrireTexte(
                ctx,
                label,
                MARGE_GAUCHE + 15,
                ctx.y - 12,
                ctx.fontRegular,
                11
        );

        ecrireTexte(
                ctx,
                formatMontant(montant),
                MARGE_GAUCHE + 15,
                ctx.y - 32,
                ctx.fontBold,
                18
        );

        // IMPORTANT :
        // on descend après le bloc
        ctx.y -=
                HAUTEUR_BLOC
                        + ESPACE_APRES;
    }

    // ============================================================
    // TABLEAU
    // ============================================================

    private static final float COL_PERIODE = 0;

    private static final float COL_HEURES = 140;

    private static final float COL_TAUX = 180;

    private static final float COL_SALAIRE = 225;

    private static final float COL_MONTANT_H = 300;

    private static final float COL_MONTANT = 370;

    private static final float COL_STATUT = 440;

    private void ecrireEnteteTableau(
            PdfContext ctx
    ) throws IOException {

        verifierSautDePage(ctx, 30);

        ecrireCellule(
                ctx,
                COL_PERIODE,
                "PERIODE",
                8.5f,
                true
        );

        ecrireCellule(
                ctx,
                COL_HEURES,
                "HEURES",
                8.5f,
                true
        );

        ecrireCellule(
                ctx,
                COL_TAUX,
                "TAUX",
                8.5f,
                true
        );

        ecrireCellule(
                ctx,
                COL_SALAIRE,
                "SALAIRE BASE",
                8.5f,
                true
        );

        ecrireCellule(
                ctx,
                COL_MONTANT_H,
                "MONT. HEURES",
                8.5f,
                true
        );

        ecrireCellule(
                ctx,
                COL_MONTANT,
                "MONTANT",
                8.5f,
                true
        );

        ecrireCellule(
                ctx,
                COL_STATUT,
                "STATUT",
                8.5f,
                true
        );

        ctx.y -= 6;

        ligneHorizontale(ctx);

        ctx.y -= 16;
    }

    private void ecrireLigneTableau(
            PdfContext ctx,
            RapportPaiementEnseignantDTO.PaiementLigneDTO ligne
    ) throws IOException {

        verifierSautDePage(ctx, 25);

        /*
         * Si une nouvelle page vient d'être créée,
         * on remet l'en-tête du tableau.
         */
        if (
                ctx.y
                        > PDRectangle.A4.getHeight()
                        - MARGE_HAUT
                        - 5
        ) {

            ecrireEnteteTableau(ctx);
        }

        String periode =
                ligne.getPeriodeDebut() != null
                        && ligne.getPeriodeFin() != null

                        ? ligne.getPeriodeDebut()
                        .format(DATE_FORMAT)
                        + " - "
                        + ligne.getPeriodeFin()
                        .format(DATE_FORMAT)

                        : "-";

        ecrireCellule(
                ctx,
                COL_PERIODE,
                periode,
                8.5f,
                false
        );

        ecrireCellule(
                ctx,
                COL_HEURES,
                ligne.getTotalHeures() + "h",
                8.5f,
                false
        );

        ecrireCellule(
                ctx,
                COL_TAUX,
                formatMontantCourt(
                        ligne.getTauxHoraire()
                ),
                8.5f,
                false
        );

        ecrireCellule(
                ctx,
                COL_SALAIRE,
                formatMontantCourt(
                        ligne.getSalaireBase()
                ),
                8.5f,
                false
        );

        ecrireCellule(
                ctx,
                COL_MONTANT_H,
                formatMontantCourt(
                        ligne.getMontantHeures()
                ),
                8.5f,
                false
        );

        ecrireCellule(
                ctx,
                COL_MONTANT,
                formatMontantCourt(
                        ligne.getMontant()
                ),
                8.5f,
                false
        );

        ecrireCellule(
                ctx,
                COL_STATUT,
                libelleStatut(
                        ligne.getStatut()
                ),
                8.5f,
                false
        );

        ctx.y -= 18;
    }

    private void ecrireCellule(
            PdfContext ctx,
            float colX,
            String texte,
            float taille,
            boolean gras
    ) throws IOException {

        ecrireTexte(
                ctx,
                texte,
                MARGE_GAUCHE + colX,
                ctx.y,
                gras
                        ? ctx.fontBold
                        : ctx.fontRegular,
                taille
        );
    }

    // ============================================================
    // PIED DE PAGE
    // ============================================================

    private void ajouterPiedDePage(
            PdfContext ctx
    ) throws IOException {

        /*
         * Le pied de page est placé en bas de la page
         * courante.
         */
        ecrireTexte(
                ctx,
                "Document généré automatiquement — rapport annuel des paiements enseignant.",
                MARGE_GAUCHE,
                MARGE_BAS - 10,
                ctx.fontRegular,
                8
        );
    }

    // ============================================================
    // FORMATAGE
    // ============================================================

    private String formatMontant(
            Double montant
    ) {

        if (montant == null) {
            return "-";
        }

        return String.format(
                        Locale.FRANCE,
                        "%,.0f FCFA",
                        montant
                )
                .replace(
                        '\u202F',
                        ' '
                )
                .replace(
                        '\u00A0',
                        ' '
                )
                .replace(
                        ",",
                        " "
                );
    }

    private String formatMontantCourt(
            Double montant
    ) {

        if (montant == null) {
            return "-";
        }

        return String.format(
                        Locale.FRANCE,
                        "%,.0f",
                        montant
                )
                .replace(
                        '\u202F',
                        ' '
                )
                .replace(
                        '\u00A0',
                        ' '
                )
                .replace(
                        ",",
                        " "
                );
    }

    private String nettoyerTexte(
            String texte
    ) {

        if (texte == null) {
            return "";
        }

        return texte
                .replace('\u202F', ' ')
                .replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u2009', ' ');
    }

    // ============================================================
    // LIBELLÉS
    // ============================================================

    private String libelleStatut(
            String statut
    ) {

        if (statut == null) {
            return "-";
        }

        return switch (statut) {

            case "PAYE" ->
                    "Payé";

            case "EN_ATTENTE" ->
                    "En attente";

            default ->
                    statut;
        };
    }

    private String libelleContrat(
            String contrat
    ) {

        if (contrat == null) {
            return "-";
        }

        return switch (contrat) {

            case "CDI" ->
                    "CDI";

            case "CDD" ->
                    "CDD";

            case "VACATAIRE" ->
                    "Vacataire";

            case "STAGIAIRE" ->
                    "Stagiaire";

            default ->
                    contrat;
        };
    }
}