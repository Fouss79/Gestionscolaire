package com.saas.school.service;
import com.saas.school.entity.*;
import com.saas.school.repository.PaiementScolariteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RecuService {

    private final PaiementScolariteRepository paiementScolariteRepository;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm", Locale.FRENCH);

    private static final PDType1Font FONT_BOLD =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final PDType1Font FONT_REGULAR =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    /**
     * Génère le reçu PDF d'un paiement déjà enregistré.
     */
    public byte[] genererRecuPdf(Long paiementId) {

        PaiementScolarite paiement = paiementScolariteRepository.findById(paiementId)
                .orElseThrow(() -> new RuntimeException("Paiement introuvable"));

        LigneFrais ligne = paiement.getLigneFrais();
        Inscription inscription = ligne.getInscription();
        Eleve eleve = inscription.getEleve();
        Ecole ecole = inscription.getEcole();

        try (PDDocument document = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float margeGauche = 55;
            float largeurPage = PDRectangle.A4.getWidth();
            float y = PDRectangle.A4.getHeight() - 60;

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {

                // ===== EN-TÊTE ÉCOLE =====
                cs.beginText();
                cs.setFont(FONT_BOLD, 16);
                cs.newLineAtOffset(margeGauche, y);
                cs.showText(ecole != null && ecole.getNom() != null ? ecole.getNom() : "École");
                cs.endText();

                y -= 30;

                // ===== TITRE =====
                cs.beginText();
                cs.setFont(FONT_BOLD, 14);
                cs.newLineAtOffset(margeGauche, y);
                cs.showText("REÇU DE PAIEMENT");
                cs.endText();

                y -= 15;

                cs.setStrokingColor(200, 200, 200);
                cs.moveTo(margeGauche, y);
                cs.lineTo(largeurPage - margeGauche, y);
                cs.stroke();

                y -= 30;

                // ===== RÉFÉRENCE + DATE =====
                y = ecrireLigne(cs, margeGauche, y, "N° reçu", paiement.getReference());
                y = ecrireLigne(
                        cs, margeGauche, y, "Date",
                        paiement.getDatePaiement() != null
                                ? paiement.getDatePaiement().format(DATE_FORMAT)
                                : "—"
                );

                y -= 15;

                // ===== ÉLÈVE =====
                y = ecrireLigne(cs, margeGauche, y, "Élève",
                        (eleve.getPrenom() != null ? eleve.getPrenom() : "") + " " +
                                (eleve.getNom() != null ? eleve.getNom() : ""));

                y = ecrireLigne(cs, margeGauche, y, "Classe",
                        inscription.getClasse() != null ? inscription.getClasse().getNomComplet() : "—");

                if (inscription.getAnneeScolaire() != null) {
                    y = ecrireLigne(cs, margeGauche, y, "Année scolaire", inscription.getAnneeScolaire().getNom());
                }

                y -= 15;

                // ===== DÉTAIL DU PAIEMENT =====
                y = ecrireLigne(cs, margeGauche, y, "Type de frais", ligne.getTypeFrais().getLibelle());

                if (paiement.getMois() != null && paiement.getAnnee() != null) {
                    y = ecrireLigne(cs, margeGauche, y, "Période",
                            NOMS_MOIS[paiement.getMois()] + " " + paiement.getAnnee());
                }

                y = ecrireLigne(cs, margeGauche, y, "Mode de paiement", libelleMode(paiement.getModePaiement()));

                y -= 20;

                // ===== MONTANT (mis en avant) =====
                cs.setStrokingColor(220, 220, 220);
                cs.addRect(margeGauche, y - 35, largeurPage - 2 * margeGauche, 45);
                cs.stroke();

                cs.beginText();
                cs.setFont(FONT_REGULAR, 11);
                cs.newLineAtOffset(margeGauche + 15, y - 12);
                cs.showText("Montant payé");
                cs.endText();

                cs.beginText();
                cs.setFont(FONT_BOLD, 18);
                cs.newLineAtOffset(margeGauche + 15, y - 30);
                cs.showText(formatMontant(paiement.getMontant()));
                cs.endText();

                y -= 70;

                // ===== PIED DE PAGE =====
                cs.beginText();
                cs.setFont(FONT_REGULAR, 9);
                cs.newLineAtOffset(margeGauche, 50);
                cs.showText("Ce reçu a été généré automatiquement et fait office de preuve de paiement.");
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération du reçu PDF", e);
        }
    }

    private float ecrireLigne(PDPageContentStream cs, float x, float y, String label, String valeur) throws IOException {

        cs.beginText();
        cs.setFont(FONT_REGULAR, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(label + " :");
        cs.endText();

        cs.beginText();
        cs.setFont(FONT_BOLD, 10);
        cs.newLineAtOffset(x + 140, y);
        cs.showText(valeur != null ? valeur : "—");
        cs.endText();

        return y - 20;
    }

    private String formatMontant(Double montant) {
        if (montant == null) return "—";
        return String.format(Locale.FRANCE, "%,.0f FCFA", montant).replace(",", " ");
    }

    private String libelleMode(String code) {
        if (code == null) return "—";
        return switch (code.toUpperCase()) {
            case "CASH" -> "Espèces";
            case "ORANGE_MONEY" -> "Orange Money";
            case "MOOV_MONEY" -> "Moov Money";
            case "VIREMENT" -> "Virement";
            case "CHEQUE" -> "Chèque";
            default -> code;
        };
    }

    private static final String[] NOMS_MOIS = {
            "", "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };
}