package com.saas.school.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.saas.school.dto.ConfigurationCreneauxDto;
import com.saas.school.entity.*;
import com.saas.school.repository.AnneeScolaireRepository;
import com.saas.school.repository.ClasseRepository;
import com.saas.school.repository.EmploiDuTempsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmploiDuTempsPdfService {

    private final EmploiDuTempsRepository edtRepo;
    private final ClasseRepository classeRepo;
    private final AnneeScolaireRepository anneeRepo;

    private final ConfigurationCreneauxService configurationCreneauxService;

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
    // ⏰ CRÉNEAUX PAR DÉFAUT
    // =========================================================

    private static final int DEFAULT_DEBUT = 8 * 60;
    private static final int DEFAULT_FIN = 18 * 60;
    private static final int DEFAULT_PAS = 60;


    // =========================================================
    // 📄 PDF D'UNE CLASSE
    // =========================================================

    public byte[] genererPdf(
            Long classeId,
            Long anneeId
    ) {

        try {

            // =====================================================
            // CLASSE
            // =====================================================

            Classe classe =
                    classeRepo.findById(classeId)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Classe introuvable"
                                    )
                            );

            // =====================================================
            // ANNÉE SCOLAIRE
            // =====================================================

            AnneeScolaire annee =
                    anneeRepo.findById(anneeId)
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
            // CONFIGURATION DES CRÉNEAUX
            // =====================================================

            List<CreneauPdf> creneaux =
                    obtenirCreneauxPourClasse(classe);

            // =====================================================
            // OUTPUT
            // =====================================================

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            // =====================================================
            // DOCUMENT A4 PAYSAGE
            // =====================================================

            Document document =
                    new Document(
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
                    emplois,
                    creneaux
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
    // ⏰ RÉCUPÉRER LES CRÉNEAUX DE LA CLASSE
    // =========================================================

    private List<CreneauPdf> obtenirCreneauxPourClasse(
            Classe classe
    ) {

        if (classe.getNiveau() == null
                || classe.getNiveau().getCycle() == null) {

            return genererCreneauxParDefaut();
        }

        Long cycleId =
                classe.getNiveau()
                        .getCycle()
                        .getId();

        Long ecoleId =
                classe.getEcole()
                        .getId();

        try {

            ConfigurationCreneauxDto config =
                    configurationCreneauxService.obtenir(
                            ecoleId,
                            cycleId
                    );

            if (config == null
                    || config.creneaux() == null
                    || config.creneaux().isEmpty()) {

                return genererCreneauxParDefaut();
            }

            return config.creneaux()
                    .stream()
                    .map(c ->
                            new CreneauPdf(
                                    c.jour(),
                                    c.heureDebut(),
                                    c.heureFin(),
                                    c.ordre()
                            )
                    )
                    .sorted(
                            Comparator
                                    .comparing(
                                            CreneauPdf::jourOrdre
                                    )
                                    .thenComparingInt(
                                            CreneauPdf::heureDebut
                                    )
                    )
                    .toList();

        } catch (Exception e) {

            /*
             * Si aucune configuration personnalisée
             * n'existe encore, on utilise les horaires
             * standards.
             */
            return genererCreneauxParDefaut();
        }
    }


    // =========================================================
    // ⏰ CRÉNEAUX PAR DÉFAUT
    // =========================================================

    private List<CreneauPdf> genererCreneauxParDefaut() {

        List<CreneauPdf> result =
                new ArrayList<>();

        for (String jour : JOURS) {

            int ordre = 1;

            for (
                    int minute = DEFAULT_DEBUT;
                    minute < DEFAULT_FIN;
                    minute += DEFAULT_PAS
            ) {

                result.add(
                        new CreneauPdf(
                                jour,
                                minute,
                                Math.min(
                                        minute + DEFAULT_PAS,
                                        DEFAULT_FIN
                                ),
                                ordre++
                        )
                );
            }
        }

        return result;
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

        String nomEcole =
                "ÉTABLISSEMENT";

        if (classe.getEcole() != null
                && classe.getEcole().getNom() != null
                && !classe.getEcole().getNom().isBlank()) {

            nomEcole =
                    classe.getEcole().getNom();
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

        Paragraph titre =
                new Paragraph(
                        "EMPLOI DU TEMPS",
                        titreFont
                );

        titre.setAlignment(
                Element.ALIGN_CENTER
        );

        document.add(titre);

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
            List<EmploiDuTemps> emplois,
            List<CreneauPdf> creneaux
    ) throws DocumentException {

        PdfPTable table =
                new PdfPTable(7);

        table.setWidthPercentage(100);

        table.setWidths(
                new float[]{
                        1.4f,
                        2.1f,
                        2.1f,
                        2.1f,
                        2.1f,
                        2.1f,
                        2.1f
                }
        );

        // =====================================================
        // HEADER
        // =====================================================

        addHeader(
                table,
                "HORAIRE"
        );

        for (String jour : JOURS) {

            addHeader(
                    table,
                    jour
            );
        }

        // =====================================================
        // GROUPEMENT DES CRÉNEAUX PAR HORAIRE
        // =====================================================

        List<CreneauPdf> bornes =
                construireLignesGlobales(creneaux);

        // =====================================================
        // ROWSPAN
        // =====================================================

        Map<String, Integer> lignesOccupees =
                new HashMap<>();

        for (String jour : JOURS) {

            lignesOccupees.put(
                    jour,
                    0
            );
        }

        // =====================================================
        // GÉNÉRATION
        // =====================================================

        for (CreneauPdf ligne : bornes) {

            // =================================================
            // HORAIRE
            // =================================================

            addHoraire(
                    table,
                    formaterPlageHoraire(
                            ligne.heureDebut(),
                            ligne.heureFin()
                    )
            );

            // =================================================
            // JOURS
            // =================================================

            for (String jour : JOURS) {

                int occupees =
                        lignesOccupees.getOrDefault(
                                jour,
                                0
                        );

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
                                ligne.heureDebut(),
                                ligne.heureFin()
                        );

                if (edt == null) {

                    table.addCell(
                            celluleVide()
                    );

                    continue;
                }

                // =================================================
                // ROWSPAN BASÉ SUR LES CRÉNEAUX RÉELS
                // =================================================

                int rowSpan =
                        calculerRowSpan(
                                creneaux,
                                jour,
                                edt,
                                bornes
                        );

                PdfPCell cellule =
                        celluleCours(edt);

                cellule.setRowspan(
                        Math.max(
                                1,
                                rowSpan
                        )
                );

                table.addCell(
                        cellule
                );

                if (rowSpan > 1) {

                    lignesOccupees.put(
                            jour,
                            rowSpan - 1
                    );
                }
            }
        }

        document.add(table);
    }


    // =========================================================
    // 🔎 CONSTRUIRE LES LIGNES DU TABLEAU
    // =========================================================

    private List<CreneauPdf> construireLignesGlobales(
            List<CreneauPdf> creneaux
    ) {

        Set<String> dejaVu =
                new HashSet<>();

        List<CreneauPdf> result =
                new ArrayList<>();

        for (CreneauPdf c : creneaux) {

            String cle =
                    c.heureDebut()
                            + "-"
                            + c.heureFin();

            if (dejaVu.add(cle)) {

                result.add(c);
            }
        }

        return result.stream()
                .sorted(
                        Comparator
                                .comparingInt(
                                        CreneauPdf::heureDebut
                                )
                                .thenComparingInt(
                                        CreneauPdf::heureFin
                                )
                )
                .toList();
    }


    // =========================================================
    // 🔎 RECHERCHE DU COURS
    // =========================================================

    private EmploiDuTemps trouverCours(
            List<EmploiDuTemps> emplois,
            String jour,
            int debut,
            int fin
    ) {

        for (EmploiDuTemps edt : emplois) {

            if (!jour.equalsIgnoreCase(
                    edt.getJour()
            )) {
                continue;
            }

            if (
                    edt.getHeureDebut() <= debut
                            &&
                            edt.getHeureFin() >= fin
            ) {

                return edt;
            }
        }

        return null;
    }


    // =========================================================
    // 🔢 ROWSPAN
    // =========================================================

    private int calculerRowSpan(
            List<CreneauPdf> creneaux,
            String jour,
            EmploiDuTemps edt,
            List<CreneauPdf> lignes
    ) {

        int rowspan = 0;

        for (CreneauPdf ligne : lignes) {

            if (
                    ligne.heureDebut()
                            >= edt.getHeureDebut()
                            &&
                            ligne.heureFin()
                                    <= edt.getHeureFin()
            ) {

                boolean existeCeJour =
                        creneaux.stream()
                                .anyMatch(c ->
                                        c.jour()
                                                .equalsIgnoreCase(jour)
                                                &&
                                                c.heureDebut()
                                                        == ligne.heureDebut()
                                                &&
                                                c.heureFin()
                                                        == ligne.heureFin()
                                );

                if (existeCeJour) {

                    rowspan++;
                }
            }
        }

        return Math.max(
                1,
                rowspan
        );
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

        Paragraph contenu =
                new Paragraph();

        contenu.setAlignment(
                Element.ALIGN_CENTER
        );

        contenu.setLeading(8);

        contenu.add(
                new Chunk(
                        matiere,
                        matiereFont
                )
        );

        if (!enseignant.isBlank()) {

            contenu.add(
                    Chunk.NEWLINE
            );

            contenu.add(
                    new Chunk(
                            enseignant,
                            detailFont
                    )
            );
        }

        if (edt.getSalle() != null) {

            contenu.add(
                    Chunk.NEWLINE
            );

            contenu.add(
                    new Chunk(
                            "Salle : "
                                    + edt.getSalle().getNom(),
                            detailFont
                    )
            );
        }

        if (edt.getSousGroupe() != null) {

            contenu.add(
                    Chunk.NEWLINE
            );

            contenu.add(
                    new Chunk(
                            "Groupe : "
                                    + edt.getSousGroupe().getNom(),
                            detailFont
                    )
            );
        }

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

        cell.setMinimumHeight(30);

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
    // 🏷️ HEADER
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
                        7,
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

        cell.setPadding(3);

        cell.setMinimumHeight(30);

        table.addCell(cell);
    }


    // =========================================================
    // ⏰ FORMAT HEURE
    // =========================================================

    private String formaterPlageHoraire(
            int debut,
            int fin
    ) {

        return formaterHeure(debut)
                + " - "
                + formaterHeure(fin);
    }


    private String formaterHeure(
            int minutes
    ) {

        int heures =
                minutes / 60;

        int minutesRestantes =
                minutes % 60;

        if (minutesRestantes == 0) {

            return String.format(
                    "%02dh",
                    heures
            );
        }

        return String.format(
                "%02dh%02d",
                heures,
                minutesRestantes
        );
    }


    // =========================================================
    // ⬜ CELLULE VIDE
    // =========================================================

    private PdfPCell celluleVide() {

        PdfPCell cell =
                new PdfPCell(
                        new Phrase("")
                );

        cell.setMinimumHeight(30);

        cell.setVerticalAlignment(
                Element.ALIGN_MIDDLE
        );

        return cell;
    }


    // =========================================================
    // 📦 OBJET INTERNE PDF
    // =========================================================

    private record CreneauPdf(
            String jour,
            int heureDebut,
            int heureFin,
            int ordre
    ) {

        private int jourOrdre() {

            for (int i = 0; i < JOURS.length; i++) {

                if (JOURS[i]
                        .equalsIgnoreCase(jour)) {

                    return i;
                }
            }

            return 99;
        }
    }
}