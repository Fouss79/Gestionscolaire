package com.saas.school.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.saas.school.entity.*;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.ClasseRepository;
import com.saas.school.repository.EmploiDuTempsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmploiDuTempsPdfService {

    private final EmploiDuTempsRepository edtRepo;
    private final ClasseRepository classeRepo;
    private final AnneeScolaireRepository anneeRepo;

    // =========================================================
    // 📅 JOURS
    // =========================================================

    private static final String[] JOURS = {
            "LUNDI",
            "MARDI",
            "MERCREDI",
            "JEUDI",
            "VENDREDI",
            "SAMEDI"
    };

    // =========================================================
    // ⏰ HORAIRES
    // =========================================================

    private static final int HEURE_DEBUT = 8;
    private static final int HEURE_FIN = 18;


    // =========================================================
    // 📄 PDF D'UNE CLASSE
    // =========================================================

    public byte[] genererPdf(Long classeId, Long anneeId) {

        try {

            // =====================================================
            // CLASSE
            // =====================================================

            Classe classe = classeRepo.findById(classeId)
                    .orElseThrow(() ->
                            new RuntimeException("Classe introuvable")
                    );

            // =====================================================
            // ANNÉE SCOLAIRE
            // =====================================================

            AnneeScolaire annee = anneeRepo.findById(anneeId)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Année scolaire introuvable"
                            )
                    );

            // =====================================================
            // EMPLOI DU TEMPS
            // =====================================================

            List<EmploiDuTemps> emplois =
                    edtRepo.findByClasseIdAndAnneeScolaireId(
                            classeId,
                            anneeId
                    );

            // =====================================================
            // OUTPUT
            // =====================================================

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            // =====================================================
            // DOCUMENT A4 PAYSAGE
            // =====================================================

            Document document = new Document(
                    PageSize.A4.rotate(),
                    25,
                    25,
                    25,
                    25
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
                    emplois
            );

            // =====================================================
            // FERMETURE
            // =====================================================

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Erreur lors de la génération du PDF",
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

        Font ecoleFont =
                new Font(
                        Font.FontFamily.HELVETICA,
                        16,
                        Font.BOLD
                );

        Font titreFont =
                new Font(
                        Font.FontFamily.HELVETICA,
                        18,
                        Font.BOLD
                );

        Font infoFont =
                new Font(
                        Font.FontFamily.HELVETICA,
                        11
                );

        // =====================================================
        // NOM DE L'ÉTABLISSEMENT
        // =====================================================

        String nomEcole = "ÉTABLISSEMENT";

        if (classe.getEcole() != null
                && classe.getEcole().getNom() != null
                && !classe.getEcole().getNom().isBlank()) {

            nomEcole = classe.getEcole().getNom();
        }

        Paragraph ecole =
                new Paragraph(
                        nomEcole,
                        ecoleFont
                );

        ecole.setAlignment(
                Element.ALIGN_CENTER
        );

        document.add(ecole);


        // =====================================================
        // TITRE
        // =====================================================

        Paragraph titre =
                new Paragraph(
                        "EMPLOI DU TEMPS",
                        titreFont
                );

        titre.setAlignment(
                Element.ALIGN_CENTER
        );

        document.add(titre);


        // =====================================================
        // CLASSE
        // =====================================================

        Paragraph classeInfo =
                new Paragraph(
                        "Classe : "
                                + classe.getNomComplet(),
                        infoFont
                );

        classeInfo.setAlignment(
                Element.ALIGN_CENTER
        );

        document.add(classeInfo);


        // =====================================================
        // ANNÉE SCOLAIRE
        // =====================================================

        Paragraph anneeInfo =
                new Paragraph(
                        "Année scolaire : "
                                + annee.getNom(),
                        infoFont
                );

        anneeInfo.setAlignment(
                Element.ALIGN_CENTER
        );

        document.add(anneeInfo);

        document.add(
                new Paragraph(" ")
        );
    }


    // =========================================================
    // 📊 TABLEAU
    // =========================================================

    private void genererTableau(
            Document document,
            List<EmploiDuTemps> emplois
    ) throws DocumentException {

        PdfPTable table = new PdfPTable(7);

        table.setWidthPercentage(100);

        table.setWidths(
                new float[]{
                        1.2f,
                        2.1f,
                        2.1f,
                        2.1f,
                        2.1f,
                        2.1f,
                        2.1f
                }
        );

        // En-tête
        addHeader(table, "HORAIRE");

        for (String jour : JOURS) {
            addHeader(table, jour);
        }

        /*
         * Indique pour chaque jour combien de lignes
         * sont encore occupées par un rowspan.
         *
         * Exemple :
         * Mathématiques 08h-11h
         *
         * LUNDI = 2 lignes restantes après la ligne 08h.
         */
        Map<String, Integer> lignesOccupees = new HashMap<>();

        for (String jour : JOURS) {
            lignesOccupees.put(jour, 0);
        }

        for (
                int heure = HEURE_DEBUT;
                heure < HEURE_FIN;
                heure++
        ) {

            // Colonne horaire
            addHoraire(
                    table,
                    String.format(
                            "%02dh - %02dh",
                            heure,
                            heure + 1
                    )
            );

            for (String jour : JOURS) {

                int occupees =
                        lignesOccupees.getOrDefault(
                                jour,
                                0
                        );

                /*
                 * Si une cellule rowspan précédente
                 * couvre cette ligne, on ne crée PAS
                 * de nouvelle cellule.
                 */
                if (occupees > 0) {

                    lignesOccupees.put(
                            jour,
                            occupees - 1
                    );

                    continue;
                }

                EmploiDuTemps edt =
                        trouverCours(
                                emplois,
                                jour,
                                heure
                        );

                if (edt == null) {

                    table.addCell(
                            celluleVide()
                    );

                } else {

                    /*
                     * Nombre d'heures du cours.
                     *
                     * Exemple :
                     * 08 -> 10 = 2 lignes
                     * 08 -> 11 = 3 lignes
                     */
                    int rowSpan =
                            edt.getHeureFin()
                                    - edt.getHeureDebut();

                    /*
                     * On ne dépasse jamais 18h.
                     */
                    rowSpan =
                            Math.min(
                                    rowSpan,
                                    HEURE_FIN
                                            - heure
                            );

                    PdfPCell cellule =
                            celluleCours(edt);

                    cellule.setRowspan(rowSpan);

                    table.addCell(cellule);

                    /*
                     * Les lignes suivantes seront
                     * couvertes par cette cellule.
                     */
                    if (rowSpan > 1) {

                        lignesOccupees.put(
                                jour,
                                rowSpan - 1
                        );
                    }
                }
            }
        }

        document.add(table);
    }

    // =========================================================
    // 🔎 RECHERCHE DU COURS
    // =========================================================

    private EmploiDuTemps trouverCours(
            List<EmploiDuTemps> emplois,
            String jour,
            int heure
    ) {

        for (EmploiDuTemps edt : emplois) {

            if (!jour.equalsIgnoreCase(
                    edt.getJour()
            )) {
                continue;
            }

            if (
                    heure >= edt.getHeureDebut()
                            &&
                            heure < edt.getHeureFin()
            ) {

                return edt;
            }
        }

        return null;
    }


    // =========================================================
    // 📚 CELLULE COURS
    // =========================================================

    private PdfPCell celluleCours(
            EmploiDuTemps edt
    ) {

        Font matiereFont =
                new Font(
                        Font.FontFamily.HELVETICA,
                        9,
                        Font.BOLD
                );

        Font detailFont =
                new Font(
                        Font.FontFamily.HELVETICA,
                        7
                );

        String matiere =
                edt.getMatiere() != null
                        ? edt.getMatiere().getNom()
                        : "Matière";

        String enseignant = "";

        if (edt.getEnseignant() != null) {
            enseignant =
                    construireNomEnseignant(
                            edt.getEnseignant()
                    );
        }

        /*
         * =====================================================
         * MATIÈRE
         * =====================================================
         */

        Paragraph pMatiere =
                new Paragraph(
                        matiere,
                        matiereFont
                );

        pMatiere.setAlignment(
                Element.ALIGN_CENTER
        );

        pMatiere.setSpacingBefore(0);
        pMatiere.setSpacingAfter(0);
        pMatiere.setLeading(9);


        /*
         * =====================================================
         * ENSEIGNANT
         * =====================================================
         */

        Paragraph pEnseignant =
                new Paragraph(
                        enseignant,
                        detailFont
                );

        pEnseignant.setAlignment(
                Element.ALIGN_CENTER
        );

        pEnseignant.setSpacingBefore(0);
        pEnseignant.setSpacingAfter(0);
        pEnseignant.setLeading(8);


        /*
         * =====================================================
         * CONTENU
         * =====================================================
         */

        Paragraph contenu =
                new Paragraph();

        contenu.setAlignment(
                Element.ALIGN_CENTER
        );

        contenu.setSpacingBefore(0);
        contenu.setSpacingAfter(0);

        contenu.add(pMatiere);
        contenu.add(pEnseignant);


        /*
         * =====================================================
         * SALLE
         * =====================================================
         */

        if (edt.getSalle() != null) {

            Paragraph salle =
                    new Paragraph(
                            "Salle : "
                                    + edt.getSalle().getNom(),
                            detailFont
                    );

            salle.setAlignment(
                    Element.ALIGN_CENTER
            );

            salle.setSpacingBefore(0);
            salle.setSpacingAfter(0);
            salle.setLeading(8);

            contenu.add(salle);
        }


        /*
         * =====================================================
         * SOUS-GROUPE
         * =====================================================
         */

        if (edt.getSousGroupe() != null) {

            Paragraph groupe =
                    new Paragraph(
                            "Groupe : "
                                    + edt.getSousGroupe().getNom(),
                            detailFont
                    );

            groupe.setAlignment(
                    Element.ALIGN_CENTER
            );

            groupe.setSpacingBefore(0);
            groupe.setSpacingAfter(0);
            groupe.setLeading(8);

            contenu.add(groupe);
        }


        /*
         * =====================================================
         * CELLULE
         * =====================================================
         */

        PdfPCell cell =
                new PdfPCell(
                        contenu
                );

        cell.setHorizontalAlignment(
                Element.ALIGN_CENTER
        );

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        cell.setPadding(3);

        cell.setMinimumHeight(42);

        return cell;
    }


    // =========================================================
    // 👨‍🏫 ENSEIGNANT
    // =========================================================

    private String construireNomEnseignant(
            Enseignant enseignant
    ) {

        StringBuilder nom =
                new StringBuilder();

        if (enseignant.getNom() != null
                && !enseignant.getNom().isBlank()) {

            nom.append(
                    enseignant.getNom()
            );
        }

        if (enseignant.getPrenom() != null
                && !enseignant.getPrenom().isBlank()) {

            if (!nom.isEmpty()) {
                nom.append(" ");
            }

            nom.append(
                    enseignant.getPrenom()
            );
        }

        return nom.toString();
    }


    // =========================================================
    // 🏷️ HEADER DU TABLEAU
    // =========================================================

    private void addHeader(
            PdfPTable table,
            String texte
    ) {

        Font font =
                new Font(
                        Font.FontFamily.HELVETICA,
                        9,
                        Font.BOLD
                );

        PdfPCell cell =
                new PdfPCell(
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
    // ⏰ HORAIRE
    // =========================================================

    private void addHoraire(
            PdfPTable table,
            String texte
    ) {

        Font font =
                new Font(
                        Font.FontFamily.HELVETICA,
                        8,
                        Font.BOLD
                );

        PdfPCell cell =
                new PdfPCell(
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

        cell.setPadding(5);

        table.addCell(cell);
    }


    // =========================================================
    // ⬜ CELLULE VIDE
    // =========================================================

    private PdfPCell celluleVide() {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase("")
                );

        cell.setMinimumHeight(42);

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        return cell;
    }
}