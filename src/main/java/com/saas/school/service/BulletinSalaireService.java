package com.saas.school.service;

import com.saas.school.entity.Ecole;
import com.saas.school.entity.Enseignant;
import com.saas.school.entity.PaiementEnseignant;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BulletinSalaireService {

    private final PaiementEnseignantService paiementEnseignantService;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private static final PDType1Font FONT_BOLD =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final PDType1Font FONT_REGULAR =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    /**
     * Génère le bulletin de salaire PDF d'un paiement enseignant déjà
     * généré (peu importe qu'il soit EN_ATTENTE ou PAYE).
     */
    public byte[] genererBulletinPdf(Long paiementId) {

        PaiementEnseignant paiement = paiementEnseignantService.getById(paiementId);

        Enseignant enseignant = paiement.getEnseignant();
        Ecole ecole = enseignant.getEcole();

        boolean estVacataire =
                enseignant.getTypeContrat() == Enseignant.TypeContrat.VACATAIRE;

        try (PDDocument document = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margeGauche = 55;
            float largeurPage = PDRectangle.A4.getWidth();
            float y = PDRectangle.A4.getHeight() - 60;

            try (PDPageContentStream cs =
                         new PDPageContentStream(document, page)) {

                // =========================================================
                // EN-TÊTE ÉCOLE
                // =========================================================
                cs.beginText();
                cs.setFont(FONT_BOLD, 16);
                cs.newLineAtOffset(margeGauche, y);

                cs.showText(safePdfText(
                        ecole != null && ecole.getNom() != null
                                ? ecole.getNom()
                                : "Ecole"
                ));

                cs.endText();

                y -= 30;

                // =========================================================
                // TITRE
                // =========================================================
                cs.beginText();
                cs.setFont(FONT_BOLD, 14);
                cs.newLineAtOffset(margeGauche, y);
                cs.showText("BULLETIN DE SALAIRE");
                cs.endText();

                y -= 15;

                // Ligne
                cs.setStrokingColor(new Color(200, 200, 200));
                cs.moveTo(margeGauche, y);
                cs.lineTo(largeurPage - margeGauche, y);
                cs.stroke();

                y -= 30;

                // =========================================================
                // PÉRIODE
                // =========================================================
                y = ecrireLigne(
                        cs,
                        margeGauche,
                        y,
                        "Periode",
                        paiement.getPeriodeDebut().format(DATE_FORMAT)
                                + " au "
                                + paiement.getPeriodeFin().format(DATE_FORMAT)
                );

                y = ecrireLigne(
                        cs,
                        margeGauche,
                        y,
                        "Statut",
                        libelleStatut(paiement.getStatut().name())
                );

                if (paiement.getDatePaiement() != null) {
                    y = ecrireLigne(
                            cs,
                            margeGauche,
                            y,
                            "Date de paiement",
                            paiement.getDatePaiement().format(DATE_FORMAT)
                    );
                }

                y -= 15;

                // =========================================================
                // ENSEIGNANT
                // =========================================================
                String nomComplet =
                        (enseignant.getPrenom() != null
                                ? enseignant.getPrenom()
                                : "")
                                + " "
                                + (enseignant.getNom() != null
                                ? enseignant.getNom()
                                : "");

                y = ecrireLigne(
                        cs,
                        margeGauche,
                        y,
                        "Enseignant",
                        nomComplet.trim()
                );

                if (enseignant.getMatricule() != null) {
                    y = ecrireLigne(
                            cs,
                            margeGauche,
                            y,
                            "Matricule",
                            enseignant.getMatricule()
                    );
                }

                if (enseignant.getSpecialite() != null) {
                    y = ecrireLigne(
                            cs,
                            margeGauche,
                            y,
                            "Specialite",
                            enseignant.getSpecialite()
                    );
                }

                y = ecrireLigne(
                        cs,
                        margeGauche,
                        y,
                        "Type de contrat",
                        libelleContrat(enseignant.getTypeContrat())
                );

                y -= 15;

                // =========================================================
                // DÉTAIL RÉMUNÉRATION
                // =========================================================
                if (estVacataire) {

                    y = ecrireLigne(
                            cs,
                            margeGauche,
                            y,
                            "Heures emargees",
                            paiement.getTotalHeures() + " h"
                    );

                    y = ecrireLigne(
                            cs,
                            margeGauche,
                            y,
                            "Taux horaire",
                            formatMontant(paiement.getTauxHoraire())
                    );

                    y = ecrireLigne(
                            cs,
                            margeGauche,
                            y,
                            "Montant heures",
                            formatMontant(paiement.getMontantHeures())
                    );

                } else {

                    y = ecrireLigne(
                            cs,
                            margeGauche,
                            y,
                            "Salaire de base",
                            formatMontant(paiement.getSalaireBase())
                    );
                }

                y -= 20;

                // =========================================================
                // MONTANT NET
                // =========================================================
                cs.setStrokingColor(new Color(220, 220, 220));

                cs.addRect(
                        margeGauche,
                        y - 35,
                        largeurPage - 2 * margeGauche,
                        45
                );

                cs.stroke();

                // Label
                cs.beginText();
                cs.setFont(FONT_REGULAR, 11);
                cs.newLineAtOffset(margeGauche + 15, y - 12);
                cs.showText("Net a payer");
                cs.endText();

                // Montant
                cs.beginText();
                cs.setFont(FONT_BOLD, 18);
                cs.newLineAtOffset(margeGauche + 15, y - 30);

                cs.showText(
                        safePdfText(formatMontant(paiement.getMontant()))
                );

                cs.endText();

                y -= 70;

                // =========================================================
                // PIED DE PAGE
                // =========================================================
                cs.beginText();
                cs.setFont(FONT_REGULAR, 9);
                cs.newLineAtOffset(margeGauche, 50);

                cs.showText(
                        "Ce bulletin a ete genere automatiquement et fait office de preuve de paiement."
                );

                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            document.save(out);

            return out.toByteArray();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erreur lors de la génération du bulletin de salaire",
                    e
            );
        }
    }

    // =========================================================
    // ÉCRITURE D'UNE LIGNE
    // =========================================================
    private float ecrireLigne(
            PDPageContentStream cs,
            float x,
            float y,
            String label,
            String valeur
    ) throws IOException {

        // Label
        cs.beginText();

        cs.setFont(FONT_REGULAR, 10);

        cs.newLineAtOffset(x, y);

        cs.showText(
                safePdfText(label + " :")
        );

        cs.endText();

        // Valeur
        cs.beginText();

        cs.setFont(FONT_BOLD, 10);

        cs.newLineAtOffset(x + 150, y);

        cs.showText(
                safePdfText(
                        valeur != null
                                ? valeur
                                : "—"
                )
        );

        cs.endText();

        return y - 20;
    }

    // =========================================================
    // FORMAT MONTANT
    // =========================================================
    private String formatMontant(Double montant) {

        if (montant == null) {
            return "-";
        }

        /*
         * On évite Locale.FRANCE ici afin de ne pas obtenir
         * automatiquement U+202F (espace insécable fine).
         */
        return String.format(
                Locale.US,
                "%,.0f FCFA",
                montant
        );
    }

    // =========================================================
    // NETTOYAGE TEXTE POUR PDFBOX
    // =========================================================
    private String safePdfText(String texte) {

        if (texte == null) {
            return "";
        }

        return texte

                // Espace insécable fine U+202F
                .replace('\u202F', ' ')

                // Espace insécable U+00A0
                .replace('\u00A0', ' ')

                // Espace fine U+2009
                .replace('\u2009', ' ')

                // Espace étroite insécable U+202F
                .replace('\u202F', ' ');
    }

    // =========================================================
    // STATUT
    // =========================================================
    private String libelleStatut(String statut) {

        return switch (statut) {

            case "PAYE" -> "Paye";

            case "EN_ATTENTE" -> "En attente";

            default -> safePdfText(statut);
        };
    }

    // =========================================================
    // TYPE CONTRAT
    // =========================================================
    private String libelleContrat(
            Enseignant.TypeContrat contrat
    ) {

        if (contrat == null) {
            return "-";
        }

        return switch (contrat) {

            case CDI -> "CDI";

            case CDD -> "CDD";

            case VACATAIRE -> "Vacataire";

            case STAGIAIRE -> "Stagiaire";
        };
    }
}