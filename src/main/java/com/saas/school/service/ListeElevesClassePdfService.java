package com.saas.school.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.saas.school.dto.InscriptionResponseDTO;
import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Classe;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.ClasseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListeElevesClassePdfService {

    private final InscriptionService inscriptionService;
    private final ClasseRepository classeRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;

    public byte[] genererPdf(
            Long classeId,
            Long anneeId
    ) {

        try {

            // =====================================================
            // CLASSE
            // =====================================================

            Classe classe = classeRepository.findById(classeId)
                    .orElseThrow(() ->
                            new RuntimeException("Classe introuvable")
                    );

            // =====================================================
            // ANNÉE SCOLAIRE
            // =====================================================

            AnneeScolaire annee = anneeScolaireRepository.findById(anneeId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Année scolaire introuvable"
                            )
                    );

            // =====================================================
            // ÉLÈVES VALIDÉS
            // =====================================================

            List<InscriptionResponseDTO> inscriptions =
                    inscriptionService.getByClasseEtAnnee(
                            classeId,
                            anneeId
                    );

            // =====================================================
            // OUTPUT
            // =====================================================

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            Document document = new Document(
                    PageSize.A4,
                    30,
                    30,
                    30,
                    30
            );

            PdfWriter.getInstance(
                    document,
                    outputStream
            );

            document.open();

            // =====================================================
            // ENTÊTE
            // =====================================================

            genererEntete(
                    document,
                    classe,
                    annee
            );

            // =====================================================
            // TABLEAU
            // =====================================================

            genererTableau(
                    document,
                    inscriptions
            );

            // =====================================================
            // FERMETURE
            // =====================================================

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Erreur lors de la génération de la liste des élèves",
                    e
            );
        }
    }

    // =========================================================
    // 🏫 ENTÊTE
    // =========================================================

    private void genererEntete(
            Document document,
            Classe classe,
            AnneeScolaire annee
    ) throws DocumentException {

        Font ecoleFont = new Font(
                Font.FontFamily.HELVETICA,
                16,
                Font.BOLD
        );

        Font titreFont = new Font(
                Font.FontFamily.HELVETICA,
                15,
                Font.BOLD
        );

        Font infoFont = new Font(
                Font.FontFamily.HELVETICA,
                10
        );

        String nomEcole = "ÉTABLISSEMENT";

        if (classe.getEcole() != null
                && classe.getEcole().getNom() != null
                && !classe.getEcole().getNom().isBlank()) {

            nomEcole = classe.getEcole().getNom();
        }

        Paragraph ecole = new Paragraph(
                nomEcole,
                ecoleFont
        );

        ecole.setAlignment(Element.ALIGN_CENTER);

        document.add(ecole);

        Paragraph titre = new Paragraph(
                "LISTE DES ÉLÈVES INSCRITS",
                titreFont
        );

        titre.setAlignment(Element.ALIGN_CENTER);

        document.add(titre);

        document.add(new Paragraph(" "));

        PdfPTable infos = new PdfPTable(2);

        infos.setWidthPercentage(100);

        infos.setWidths(new float[]{
                1,
                1
        });

        infos.addCell(celluleInfo(
                "Classe : " + classe.getNomComplet()
        ));

        infos.addCell(celluleInfo(
                "Année scolaire : " + annee.getNom()
        ));

        document.add(infos);

        document.add(new Paragraph(" "));
    }

    // =========================================================
    // 📋 TABLEAU DES ÉLÈVES
    // =========================================================

    private void genererTableau(
            Document document,
            List<InscriptionResponseDTO> inscriptions
    ) throws DocumentException {

        PdfPTable table = new PdfPTable(6);

        table.setWidthPercentage(100);

        table.setWidths(new float[]{
                0.6f,
                1.5f,
                2.2f,
                2.2f,
                0.8f,
                1.5f
        });

        // -----------------------------------------------------
        // EN-TÊTE
        // -----------------------------------------------------

        addHeader(table, "N°");
        addHeader(table, "MATRICULE");
        addHeader(table, "NOM");
        addHeader(table, "PRÉNOM");
        addHeader(table, "SEXE");
        addHeader(table, "DATE NAISS.");

        // -----------------------------------------------------
        // LIGNES
        // -----------------------------------------------------

        int numero = 1;

        for (InscriptionResponseDTO inscription : inscriptions) {

            addCell(table, String.valueOf(numero));

            addCell(
                    table,
                    valeur(inscription.getMatricule())
            );

            addCell(
                    table,
                    valeur(inscription.getNom())
            );

            addCell(
                    table,
                    valeur(inscription.getPrenom())
            );

            addCell(
                    table,
                    valeur(inscription.getSexe())
            );

            String dateNaissance = "";

            if (inscription.getDateNaissance() != null) {
                dateNaissance =
                        inscription.getDateNaissance().toString();
            }

            addCell(table, dateNaissance);

            numero++;
        }

        // -----------------------------------------------------
        // AUCUN ÉLÈVE
        // -----------------------------------------------------

        if (inscriptions.isEmpty()) {

            PdfPCell cellule = new PdfPCell(
                    new Phrase(
                            "Aucun élève inscrit dans cette classe."
                    )
            );

            cellule.setColspan(6);
            cellule.setHorizontalAlignment(
                    Element.ALIGN_CENTER
            );

            cellule.setPadding(8);

            table.addCell(cellule);
        }

        document.add(table);

        // -----------------------------------------------------
        // TOTAL
        // -----------------------------------------------------

        document.add(new Paragraph(" "));

        Font totalFont = new Font(
                Font.FontFamily.HELVETICA,
                10,
                Font.BOLD
        );

        Paragraph total = new Paragraph(
                "Total des élèves : " + inscriptions.size(),
                totalFont
        );

        total.setAlignment(Element.ALIGN_RIGHT);

        document.add(total);
    }

    // =========================================================
    // CELLULE EN-TÊTE
    // =========================================================

    private void addHeader(
            PdfPTable table,
            String texte
    ) {

        Font font = new Font(
                Font.FontFamily.HELVETICA,
                9,
                Font.BOLD
        );

        PdfPCell cell = new PdfPCell(
                new Phrase(
                        texte,
                        font
                )
        );

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(6);

        table.addCell(cell);
    }

    // =========================================================
    // CELLULE NORMALE
    // =========================================================

    private void addCell(
            PdfPTable table,
            String texte
    ) {

        Font font = new Font(
                Font.FontFamily.HELVETICA,
                9
        );

        PdfPCell cell = new PdfPCell(
                new Phrase(
                        texte,
                        font
                )
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(5);

        table.addCell(cell);
    }

    // =========================================================
    // CELLULE INFO
    // =========================================================

    private PdfPCell celluleInfo(
            String texte
    ) {

        Font font = new Font(
                Font.FontFamily.HELVETICA,
                10,
                Font.BOLD
        );

        PdfPCell cell = new PdfPCell(
                new Phrase(
                        texte,
                        font
                )
        );

        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);

        return cell;
    }

    // =========================================================
    // VALEUR
    // =========================================================

    private String valeur(String valeur) {

        return valeur != null
                ? valeur
                : "";
    }
}