package com.saas.school.service;

import com.saas.school.entity.Classe;
import com.saas.school.entity.CoefficientMatiere;
import com.saas.school.entity.Inscription;
import com.saas.school.entity.Note;
import com.saas.school.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteExcelService {

    private final InscriptionRepository inscriptionRepository;
    private final CoefficientMatiereRepository coefficientMatiereRepository;
    private final NoteRepository noteRepository;
    private final ClasseRepository classeRepository;
    private final AnneeScolaireRepository anneeScolaireRepository;

    /**
     * Génère le fichier Excel pour une classe,
     * une matière et une période.
     */
    public byte[] exporter(
            Long classeId,
            Long anneeId,
            Long coefficientMatiereId,
            String periode
    ) throws IOException {

        CoefficientMatiere programme =
                coefficientMatiereRepository.findById(coefficientMatiereId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Coefficient matière introuvable : "
                                                + coefficientMatiereId
                                ));

        /*
         * Vérification importante :
         * le programme doit correspondre à la classe demandée.
         */
        if (programme.getClasse() == null ||
                !programme.getClasse().getId().equals(classeId)) {

            throw new RuntimeException(
                    "Cette matière n'est pas associée à cette classe."
            );
        }

        List<Inscription> inscriptions =
                inscriptionRepository.findActifsByClasseAndAnnee(
                        classeId,
                        anneeId,
                        StatutInscription.VALIDE
                );

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet notesSheet = workbook.createSheet("NOTES");
            Sheet infosSheet = workbook.createSheet("INFOS");

            // =====================================================
            // STYLES
            // =====================================================

            CellStyle headerStyle = workbook.createCellStyle();

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);

            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // =====================================================
            // FEUILLE NOTES
            // =====================================================

            Row header = notesSheet.createRow(0);

            String[] headers = {
                    "N°",
                    "ID Inscription",
                    "Nom",
                    "Prénom",
                    "Note classe",
                    "Composition"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            int numero = 1;

            for (Inscription inscription : inscriptions) {

                Row row = notesSheet.createRow(rowIndex++);

                row.createCell(0).setCellValue(numero++);

                row.createCell(1).setCellValue(
                        inscription.getId()
                );

                String nom = "";
                String prenom = "";

                if (inscription.getEleve() != null) {

                    if (inscription.getEleve().getNom() != null) {
                        nom = inscription.getEleve().getNom();
                    }

                    if (inscription.getEleve().getPrenom() != null) {
                        prenom = inscription.getEleve().getPrenom();
                    }
                }

                row.createCell(2).setCellValue(nom);
                row.createCell(3).setCellValue(prenom);

                // Cellules laissées vides pour le professeur
                row.createCell(4).setBlank();
                row.createCell(5).setBlank();
            }

            // Largeurs
            notesSheet.setColumnWidth(0, 8 * 256);
            notesSheet.setColumnWidth(1, 18 * 256);
            notesSheet.setColumnWidth(2, 25 * 256);
            notesSheet.setColumnWidth(3, 25 * 256);
            notesSheet.setColumnWidth(4, 18 * 256);
            notesSheet.setColumnWidth(5, 18 * 256);

            notesSheet.createFreezePane(0, 1);

            // =====================================================
            // FEUILLE INFOS
            // =====================================================

            int infoRow = 0;

            ajouterInfo(
                    infosSheet,
                    infoRow++,
                    "Classe ID",
                    classeId
            );

            ajouterInfo(
                    infosSheet,
                    infoRow++,
                    "CoefficientMatiere ID",
                    coefficientMatiereId
            );

            ajouterInfo(
                    infosSheet,
                    infoRow++,
                    "Période",
                    periode
            );

            ajouterInfo(
                    infosSheet,
                    infoRow++,
                    "Année scolaire ID",
                    anneeId
            );

            if (programme.getClasse() != null) {
                ajouterInfo(
                        infosSheet,
                        infoRow++,
                        "Classe",
                        programme.getClasse().getNomComplet()
                );
            }

            if (programme.getMatiere() != null) {
                ajouterInfo(
                        infosSheet,
                        infoRow++,
                        "Matière",
                        programme.getMatiere().getNom()
                );
            }

            if (programme.getSousGroupe() != null) {

                ajouterInfo(
                        infosSheet,
                        infoRow++,
                        "Sous-groupe ID",
                        programme.getSousGroupe().getId()
                );

                ajouterInfo(
                        infosSheet,
                        infoRow++,
                        "Sous-groupe",
                        programme.getSousGroupe().getNom()
                );
            }

            double noteMaximale = getNoteMaximale(programme);

            ajouterInfo(
                    infosSheet,
                    infoRow,
                    "Note maximale",
                    noteMaximale
            );
            infosSheet.setColumnWidth(0, 30 * 256);
            infosSheet.setColumnWidth(1, 35 * 256);

            // =====================================================
            // EXPORT
            // =====================================================

            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

                workbook.write(output);

                return output.toByteArray();
            }
        }
    }

    private void ajouterInfo(
            Sheet sheet,
            int rowIndex,
            String nom,
            Object valeur
    ) {

        Row row = sheet.createRow(rowIndex);

        row.createCell(0).setCellValue(nom);

        if (valeur instanceof Number number) {
            row.createCell(1).setCellValue(number.doubleValue());
        } else {
            row.createCell(1).setCellValue(
                    valeur != null ? valeur.toString() : ""
            );
        }
    }

    // =============================================================
    // IMPORT
    // =============================================================

    public int importer(
            MultipartFile file,
            Long classeId,
            Long anneeId,
            Long coefficientMatiereId,
            String periode
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Le fichier Excel est vide.");
        }

        CoefficientMatiere programme =
                coefficientMatiereRepository.findById(coefficientMatiereId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Coefficient matière introuvable."
                                ));

        if (programme.getClasse() == null ||
                !programme.getClasse().getId().equals(classeId)) {

            throw new RuntimeException(
                    "La matière ne correspond pas à la classe."
            );
        }

        int nombreEnregistres = 0;

        try (Workbook workbook =
                     WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheet("NOTES");

            if (sheet == null) {
                throw new RuntimeException(
                        "La feuille NOTES est introuvable."
                );
            }

            /*
             * On commence à la ligne 1 car la ligne 0
             * contient les en-têtes.
             */
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                Long inscriptionId =
                        lireLong(row.getCell(1));

                if (inscriptionId == null) {
                    continue;
                }

                Double noteClasse =
                        lireDouble(row.getCell(4));

                Double composition =
                        lireDouble(row.getCell(5));

                /*
                 * Ligne vide de notes :
                 * on ne crée pas de Note.
                 */
                if (noteClasse == null && composition == null) {
                    continue;
                }

                Inscription inscription =
                        inscriptionRepository.findById(inscriptionId)
                                .orElseThrow(() ->
                                        new RuntimeException(
                                                "Inscription introuvable : "
                                                        + inscriptionId
                                        ));

                /*
                 * Sécurité :
                 * l'inscription doit appartenir à la classe
                 * et à l'année envoyées.
                 */
                if (inscription.getClasse() == null ||
                        !inscription.getClasse()
                                .getId()
                                .equals(classeId)) {

                    throw new RuntimeException(
                            "L'inscription "
                                    + inscriptionId
                                    + " n'appartient pas à la classe."
                    );
                }

                if (inscription.getAnneeScolaire() == null ||
                        !inscription.getAnneeScolaire()
                                .getId()
                                .equals(anneeId)) {

                    throw new RuntimeException(
                            "L'inscription "
                                    + inscriptionId
                                    + " n'appartient pas à l'année scolaire."
                    );
                }

                /*
                 * Vérification des valeurs.
                 *
                 * Pour l'instant 20.
                 * Nous rendrons cette valeur dynamique
                 * primaire / secondaire juste après.
                 */
                double noteMaximale = getNoteMaximale(programme);

                verifierNote(
                        noteClasse,
                        noteMaximale,
                        "Note classe",
                        inscriptionId
                );

                verifierNote(
                        composition,
                        noteMaximale,
                        "Composition",
                        inscriptionId
                );
                Optional<Note> existante;

                if (programme.getSousGroupe() != null) {

                    existante =
                            noteRepository
                                    .findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeId(
                                            inscriptionId,
                                            coefficientMatiereId,
                                            periode,
                                            programme.getSousGroupe().getId()
                                    );

                } else {

                    existante =
                            noteRepository
                                    .findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeIsNull(
                                            inscriptionId,
                                            coefficientMatiereId,
                                            periode
                                    );
                }

                Note note = existante.orElseGet(Note::new);

                note.setInscription(inscription);
                note.setEleve(inscription.getEleve());
                note.setClasse(inscription.getClasse());
                note.setAnneeScolaire(inscription.getAnneeScolaire());

                note.setCoefficientMatiere(programme);
                note.setMatiere(programme.getMatiere());
                note.setCoeff(programme.getCoefficient());

                note.setPeriode(periode);

                note.setNClass(noteClasse);
                note.setNExem(composition);

                note.setSousGroupe(programme.getSousGroupe());

                noteRepository.save(note);

                nombreEnregistres++;
            }
        }

        return nombreEnregistres;
    }

    private void verifierNote(
            Double valeur,
            double maximum,
            String nom,
            Long inscriptionId
    ) {

        if (valeur == null) {
            return;
        }

        if (valeur < 0 || valeur > maximum) {

            throw new RuntimeException(
                    nom
                            + " invalide pour l'inscription "
                            + inscriptionId
                            + ". Valeur autorisée : 0 à "
                            + maximum
            );
        }
    }

    private Long lireLong(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return (long) cell.getNumericCellValue();
        }

        if (cell.getCellType() == CellType.STRING) {

            String value = cell.getStringCellValue().trim();

            if (value.isEmpty()) {
                return null;
            }

            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new RuntimeException(
                        "ID inscription invalide : " + value
                );
            }
        }

        return null;
    }

    private Double lireDouble(Cell cell) {

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }

        if (cell.getCellType() == CellType.STRING) {

            String value =
                    cell.getStringCellValue()
                            .trim()
                            .replace(",", ".");

            if (value.isEmpty()) {
                return null;
            }

            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                throw new RuntimeException(
                        "Note invalide : " + value
                );
            }
        }

        return null;
    }
    private double getNoteMaximale(CoefficientMatiere programme) {

        if (programme.getClasse() == null ||
                programme.getClasse().getNiveau() == null ||
                programme.getClasse().getNiveau().getCycle() == null) {

            return 20;
        }

        String cycleNom = programme.getClasse()
                .getNiveau()
                .getCycle()
                .getNom();

        if (cycleNom != null &&
                cycleNom.trim().equalsIgnoreCase("PREMIER CYCLE")) {

            return 10;
        }

        return 20;
    }
    /**
     * =========================================================
     * 🎒 IMPORT EXCEL PRIMAIRE
     * =========================================================
     */
    @Transactional
    public int importerPrimaire(
            MultipartFile file,
            Long classeId,
            Long anneeId,
            String mois
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException(
                    "Le fichier Excel est vide."
            );
        }

        Classe classe =
                classeRepository.findById(classeId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Classe introuvable : "
                                                + classeId
                                )
                        );

        anneeScolaireRepository.findById(anneeId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Année scolaire introuvable : "
                                        + anneeId
                        )
                );

        Long ecoleId =
                classe.getEcole().getId();

        Long niveauId =
                classe.getNiveau().getId();

        // =========================================================
        // MATIÈRES AUTORISÉES
        // =========================================================

        List<CoefficientMatiere> programmes =
                coefficientMatiereRepository
                        .findProgrammesPourClasse(
                                ecoleId,
                                anneeId,
                                niveauId,
                                classeId
                        )
                        .stream()
                        .filter(cm ->
                                cm.getSousGroupe() == null
                        )
                        .sorted(
                                Comparator
                                        .comparing(
                                                (CoefficientMatiere cm) ->
                                                        cm.getMatiere().getNom()
                                        )
                                        .thenComparing(
                                                CoefficientMatiere::getId
                                        )
                        )
                        .toList();

        if (programmes.isEmpty()) {
            throw new RuntimeException(
                    "Aucune matière n'est configurée pour cette classe."
            );
        }

        /*
         * Correspondance :
         *
         * colonne Excel 4 = matière 0
         * colonne Excel 5 = matière 1
         * colonne Excel 6 = matière 2
         * ...
         */

        int nombreEnregistres = 0;

        try (
                Workbook workbook =
                        WorkbookFactory.create(
                                file.getInputStream()
                        )
        ) {

            Sheet sheet =
                    workbook.getSheet("NOTES");

            if (sheet == null) {
                throw new RuntimeException(
                        "La feuille NOTES est introuvable."
                );
            }

            // =====================================================
            // ÉLÈVES AUTORISÉS
            // =====================================================

            List<Inscription> inscriptions =
                    inscriptionRepository
                            .findElevesValidesPourReleve(
                                    classeId,
                                    anneeId
                            );

            Map<Long, Inscription> inscriptionsMap =
                    inscriptions.stream()
                            .collect(
                                    Collectors.toMap(
                                            Inscription::getId,
                                            i -> i
                                    )
                            );

            // =====================================================
            // PARCOURIR LES LIGNES
            // =====================================================

            for (
                    int rowIndex = 1;
                    rowIndex <= sheet.getLastRowNum();
                    rowIndex++
            ) {

                Row row =
                        sheet.getRow(rowIndex);

                if (row == null) {
                    continue;
                }

                Long inscriptionId =
                        lireLong(row.getCell(1));

                if (inscriptionId == null) {
                    continue;
                }

                Inscription inscription =
                        inscriptionsMap.get(
                                inscriptionId
                        );

                if (inscription == null) {

                    throw new RuntimeException(
                            "L'inscription "
                                    + inscriptionId
                                    + " n'appartient pas aux élèves valides de cette classe et de cette année."
                    );
                }

                // =================================================
                // MATIÈRES
                // =================================================

                for (
                        int i = 0;
                        i < programmes.size();
                        i++
                ) {

                    int columnIndex =
                            4 + i;

                    Double valeur =
                            lireDouble(
                                    row.getCell(
                                            columnIndex
                                    )
                            );

                    CoefficientMatiere coefficient =
                            programmes.get(i);

                    // ---------------------------------------------
                    // NOTE VIDE
                    // ---------------------------------------------

                    if (valeur == null) {

                        noteRepository
                                .findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeIsNull(
                                        inscriptionId,
                                        coefficient.getId(),
                                        mois
                                )
                                .ifPresent(
                                        noteRepository::delete
                                );

                        continue;
                    }

                    // ---------------------------------------------
                    // VALIDATION /10
                    // ---------------------------------------------

                    if (valeur < 0 || valeur > 10) {

                        throw new RuntimeException(
                                "Note invalide : "
                                        + valeur
                                        + " pour "
                                        + inscription.getEleve().getNom()
                                        + " "
                                        + inscription.getEleve().getPrenom()
                                        + ", matière : "
                                        + coefficient.getMatiere().getNom()
                                        + ". La note doit être comprise entre 0 et 10."
                        );
                    }

                    // ---------------------------------------------
                    // EXISTANTE ?
                    // ---------------------------------------------

                    Note note =
                            noteRepository
                                    .findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeIsNull(
                                            inscriptionId,
                                            coefficient.getId(),
                                            mois
                                    )
                                    .orElse(
                                            new Note()
                                    );

                    // ---------------------------------------------
                    // DONNÉES NOTE
                    // ---------------------------------------------

                    note.setInscription(
                            inscription
                    );

                    note.setEleve(
                            inscription.getEleve()
                    );

                    note.setClasse(
                            inscription.getClasse()
                    );

                    note.setAnneeScolaire(
                            inscription.getAnneeScolaire()
                    );

                    note.setMatiere(
                            coefficient.getMatiere()
                    );

                    note.setCoefficientMatiere(
                            coefficient
                    );

                    note.setPeriode(
                            mois
                    );

                    note.setNClass(
                            valeur
                    );

                    note.setNExem(
                            null
                    );

                    note.setCoeff(
                            coefficient.getCoefficient() != null
                                    ? coefficient.getCoefficient()
                                    : 1
                    );

                    note.setSousGroupe(
                            null
                    );

                    noteRepository.save(note);

                    nombreEnregistres++;
                }
            }
        }

        return nombreEnregistres;
    }
    public byte[] exporterPrimaire(
            Long classeId,
            Long anneeId,
            String mois
    ) throws IOException {

        Classe classe = classeRepository.findById(classeId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Classe introuvable : " + classeId
                        )
                );

        anneeScolaireRepository.findById(anneeId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Année scolaire introuvable : " + anneeId
                        )
                );

        Long ecoleId = classe.getEcole().getId();
        Long niveauId = classe.getNiveau().getId();

        // Élèves valides
        List<Inscription> inscriptions =
                inscriptionRepository.findElevesValidesPourReleve(
                        classeId,
                        anneeId
                );

        // Matières du programme primaire
        List<CoefficientMatiere> programmes =
                coefficientMatiereRepository
                        .findProgrammesPourClasse(
                                ecoleId,
                                anneeId,
                                niveauId,
                                classeId
                        )
                        .stream()
                        .filter(cm -> cm.getSousGroupe() == null)
                        .sorted(
                                Comparator
                                        .comparing(
                                                (CoefficientMatiere cm) ->
                                                        cm.getMatiere().getNom()
                                        )
                                        .thenComparing(
                                                CoefficientMatiere::getId
                                        )
                        )
                        .toList();

        if (programmes.isEmpty()) {
            throw new RuntimeException(
                    "Aucune matière n'est configurée pour cette classe."
            );
        }

        // Notes déjà enregistrées pour le mois
        List<Long> inscriptionIds =
                inscriptions.stream()
                        .map(Inscription::getId)
                        .toList();

        List<Note> notesExistantes =
                inscriptionIds.isEmpty()
                        ? List.of()
                        : noteRepository.findNotesPrimaire(
                        inscriptionIds,
                        anneeId,
                        mois
                );

        Map<String, Note> notesMap =
                notesExistantes.stream()
                        .collect(
                                Collectors.toMap(
                                        note ->
                                                note.getInscription().getId()
                                                        + "_"
                                                        + note.getCoefficientMatiere().getId(),
                                        note -> note,
                                        (a, b) -> a
                                )
                        );

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet notesSheet =
                    workbook.createSheet("NOTES");

            Sheet infosSheet =
                    workbook.createSheet("INFOS");

            // Style
            CellStyle headerStyle =
                    workbook.createCellStyle();

            Font headerFont =
                    workbook.createFont();

            headerFont.setBold(true);

            headerStyle.setFont(headerFont);

            headerStyle.setAlignment(
                    HorizontalAlignment.CENTER
            );

            headerStyle.setVerticalAlignment(
                    VerticalAlignment.CENTER
            );

            // =====================================================
            // EN-TÊTE
            // =====================================================

            Row header = notesSheet.createRow(0);

            String[] baseHeaders = {
                    "N°",
                    "ID Inscription",
                    "Nom",
                    "Prénom"
            };

            for (int i = 0; i < baseHeaders.length; i++) {

                Cell cell = header.createCell(i);

                cell.setCellValue(baseHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            // Matières
            for (int i = 0; i < programmes.size(); i++) {

                Cell cell =
                        header.createCell(4 + i);

                cell.setCellValue(
                        programmes.get(i)
                                .getMatiere()
                                .getNom()
                );

                cell.setCellStyle(headerStyle);
            }

            // =====================================================
            // ÉLÈVES
            // =====================================================

            int rowIndex = 1;
            int numero = 1;

            for (Inscription inscription : inscriptions) {

                Row row =
                        notesSheet.createRow(rowIndex++);

                row.createCell(0)
                        .setCellValue(numero++);

                row.createCell(1)
                        .setCellValue(inscription.getId());

                String nom = "";
                String prenom = "";

                if (inscription.getEleve() != null) {

                    nom =
                            inscription.getEleve().getNom() != null
                                    ? inscription.getEleve().getNom()
                                    : "";

                    prenom =
                            inscription.getEleve().getPrenom() != null
                                    ? inscription.getEleve().getPrenom()
                                    : "";
                }

                row.createCell(2)
                        .setCellValue(nom);

                row.createCell(3)
                        .setCellValue(prenom);

                // =================================================
                // NOTES
                // =================================================

                for (int i = 0; i < programmes.size(); i++) {

                    CoefficientMatiere programme =
                            programmes.get(i);

                    String key =
                            inscription.getId()
                                    + "_"
                                    + programme.getId();

                    Note note =
                            notesMap.get(key);

                    Cell cell =
                            row.createCell(4 + i);

                    if (note != null &&
                            note.getNClass() != null) {

                        cell.setCellValue(
                                note.getNClass()
                        );
                    }
                }
            }

            // =====================================================
            // LARGEURS
            // =====================================================

            notesSheet.setColumnWidth(
                    0, 8 * 256
            );

            notesSheet.setColumnWidth(
                    1, 18 * 256
            );

            notesSheet.setColumnWidth(
                    2, 25 * 256
            );

            notesSheet.setColumnWidth(
                    3, 25 * 256
            );

            for (int i = 0; i < programmes.size(); i++) {

                notesSheet.setColumnWidth(
                        4 + i,
                        20 * 256
                );
            }

            notesSheet.createFreezePane(4, 1);

            // =====================================================
            // INFORMATIONS
            // =====================================================

            int infoRow = 0;

            ajouterInfo(
                    infosSheet,
                    infoRow++,
                    "Type",
                    "PRIMAIRE"
            );

            ajouterInfo(
                    infosSheet,
                    infoRow++,
                    "Classe ID",
                    classeId
            );

            ajouterInfo(
                    infosSheet,
                    infoRow++,
                    "Classe",
                    classe.getNomComplet()
            );

            ajouterInfo(
                    infosSheet,
                    infoRow++,
                    "Année scolaire ID",
                    anneeId
            );

            ajouterInfo(
                    infosSheet,
                    infoRow++,
                    "Mois",
                    mois
            );

            ajouterInfo(
                    infosSheet,
                    infoRow++,
                    "Note maximale",
                    10
            );

            infosSheet.setColumnWidth(
                    0,
                    30 * 256
            );

            infosSheet.setColumnWidth(
                    1,
                    35 * 256
            );

            // =====================================================
            // GÉNÉRATION
            // =====================================================

            try (
                    ByteArrayOutputStream output =
                            new ByteArrayOutputStream()
            ) {

                workbook.write(output);

                return output.toByteArray();
            }
        }
    }
}