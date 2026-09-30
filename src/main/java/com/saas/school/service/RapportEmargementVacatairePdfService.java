package com.saas.school.service;

import com.saas.school.entity.Emargement;
import com.saas.school.entity.EmploiDuTemps;
import com.saas.school.entity.Enseignant;
import com.saas.school.repository.EmargementRepository;
import com.saas.school.repository.EmploiDuTempsRepository;
import com.saas.school.repository.EnseignantRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RapportEmargementVacatairePdfService {

    private final EnseignantRepository enseignantRepository;
    private final EmploiDuTempsRepository emploiDuTempsRepository;
    private final EmargementRepository emargementRepository;

    private static final PDRectangle PAGE =
            new PDRectangle(
                    PDRectangle.A4.getHeight(),
                    PDRectangle.A4.getWidth()
            );

    private static final float MARGIN = 25;

    private static final float HEADER_HEIGHT = 115;

    private static final float TABLE_HEADER_HEIGHT = 25;

    private static final float ROW_HEIGHT = 28;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter MOIS_FORMAT =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    // =========================================================
    // PUBLIC
    // =========================================================

    public byte[] genererPdfEnseignant(
            Long enseignantId,
            Long anneeId,
            int mois,
            int annee
    ) throws IOException {

        YearMonth yearMonth;

        try {
            yearMonth = YearMonth.of(annee, mois);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Mois ou année invalide."
            );
        }

        Enseignant enseignant =
                enseignantRepository.findById(enseignantId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Enseignant introuvable."
                                )
                        );

        // =====================================================
        // SEULS LES VACATAIRES
        // =====================================================

        if (enseignant.getTypeContrat()
                != Enseignant.TypeContrat.VACATAIRE) {

            throw new IllegalArgumentException(
                    "La fiche d'émargement mensuelle est réservée aux enseignants vacataires."
            );
        }

        // =====================================================
        // EMPLOI DU TEMPS DE L'ENSEIGNANT
        // =====================================================

        List<EmploiDuTemps> emplois =
                emploiDuTempsRepository
                        .findByEnseignant_IdAndAnneeScolaireId(
                                enseignantId,
                                anneeId
                        );

        if (emplois == null) {
            emplois = new ArrayList<>();
        }

        // =====================================================
        // PÉRIODE DU MOIS
        // =====================================================

        LocalDate debutMois =
                yearMonth.atDay(1);

        LocalDate finMois =
                yearMonth.atEndOfMonth();

        // =====================================================
        // ÉMARGEMENTS EXISTANTS DU MOIS
        //
        // On les récupère tous en une seule requête.
        // =====================================================

        List<Emargement> emargements =
                emargementRepository
                        .findByEnseignant_IdAndAnneeScolaire_IdAndDateHeureBetweenOrderByDateHeureAsc(
                                enseignantId,
                                anneeId,
                                debutMois,
                                finMois
                        );

        /*
         * Clé :
         *
         * emploiDuTempsId + "_" + date
         *
         * Cela permet de retrouver rapidement
         * l'émargement correspondant à chaque cours prévu.
         */
        Map<String, Emargement> emargementsParSeance =
                new HashMap<>();

        for (Emargement emargement : emargements) {

            if (emargement.getEmploiDuTemps() == null) {
                continue;
            }

            if (emargement.getDateHeure() == null) {
                continue;
            }

            String cle =
                    construireCle(
                            emargement.getEmploiDuTemps().getId(),
                            emargement.getDateHeure()
                    );

            emargementsParSeance.put(
                    cle,
                    emargement
            );
        }

        // =====================================================
        // GÉNÉRATION DES SÉANCES DU MOIS
        // =====================================================

        List<SeanceMensuelle> seances =
                genererSeancesDuMois(
                        emplois,
                        debutMois,
                        finMois,
                        emargementsParSeance
                );

        // =====================================================
        // TRI
        // =====================================================

        seances.sort(
                Comparator
                        .comparing(SeanceMensuelle::date)
                        .thenComparing(
                                SeanceMensuelle::heureDebut
                        )
                        .thenComparing(
                                SeanceMensuelle::classe
                        )
        );

        // =====================================================
        // PDF
        // =====================================================

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {

            PDFont font = chargerPolice(
                    document,
                    "/fonts/DejaVuSans.ttf"
            );

            PDFont fontBold = chargerPolice(
                    document,
                    "/fonts/DejaVuSans-Bold.ttf"
            );

            if (seances.isEmpty()) {

                genererPdfSansCours(
                        document,
                        font,
                        fontBold,
                        enseignant,
                        yearMonth
                );

            } else {

                genererPages(
                        document,
                        font,
                        fontBold,
                        enseignant,
                        yearMonth,
                        seances
                );
            }

            document.save(output);

            return output.toByteArray();
        }
    }

    // =========================================================
    // GÉNÉRATION DES SÉANCES
    // =========================================================

    private List<SeanceMensuelle> genererSeancesDuMois(
            List<EmploiDuTemps> emplois,
            LocalDate debut,
            LocalDate fin,
            Map<String, Emargement> emargementsParSeance
    ) {

        List<SeanceMensuelle> resultats =
                new ArrayList<>();

        for (EmploiDuTemps emploi : emplois) {

            if (emploi == null) {
                continue;
            }

            if (emploi.getJour() == null) {
                continue;
            }

            if (emploi.getHeureFin()
                    <= emploi.getHeureDebut()) {

                continue;
            }

            DayOfWeek jourCours =
                    convertirJour(
                            emploi.getJour()
                    );

            if (jourCours == null) {
                continue;
            }

            LocalDate date = debut;

            while (!date.isAfter(fin)) {

                if (date.getDayOfWeek() == jourCours) {

                    String cle =
                            construireCle(
                                    emploi.getId(),
                                    date
                            );

                    Emargement emargement =
                            emargementsParSeance.get(cle);

                    resultats.add(
                            construireSeance(
                                    emploi,
                                    date,
                                    emargement
                            )
                    );
                }

                date = date.plusDays(1);
            }
        }

        return resultats;
    }

    // =========================================================
    // CONSTRUCTION D'UNE SÉANCE
    // =========================================================

    private SeanceMensuelle construireSeance(
            EmploiDuTemps emploi,
            LocalDate date,
            Emargement emargement
    ) {

        int duree =
                emploi.getHeureFin()
                        - emploi.getHeureDebut();

        boolean emarge =
                emargement != null;

        boolean present =
                emargement != null
                        && emargement.isPresent();

        return new SeanceMensuelle(
                date,
                jourFrancais(date),
                emploi.getClasse() != null
                        ? emploi.getClasse().getNomComplet()
                        : "",
                emploi.getMatiere() != null
                        ? emploi.getMatiere().getNom()
                        : "",
                emploi.getHeureDebut(),
                emploi.getHeureFin(),
                duree,
                emarge,
                present
        );
    }

    // =========================================================
    // PAGES PDF
    // =========================================================

    private void genererPages(
            PDDocument document,
            PDFont font,
            PDFont fontBold,
            Enseignant enseignant,
            YearMonth yearMonth,
            List<SeanceMensuelle> seances
    ) throws IOException {

        int index = 0;

        int totalSeances = seances.size();

        long totalEmargees =
                seances.stream()
                        .filter(SeanceMensuelle::emargee)
                        .count();

        int totalMinutesEmarges =
                seances.stream()
                        .filter(SeanceMensuelle::emargee)
                        .mapToInt(SeanceMensuelle::duree)
                        .sum();

        double tauxHoraire =
                enseignant.getTauxHoraire() != null
                        ? enseignant.getTauxHoraire()
                        : 0;

        double montantTotal =
                (totalMinutesEmarges / 60.0)
                        * tauxHoraire;

        while (index < totalSeances) {

            PDPage page =
                    new PDPage(PAGE);

            document.addPage(page);

            int debutPage = index;

            float y =
                    PAGE.getHeight()
                            - MARGIN
                            - HEADER_HEIGHT;

            try (
                    PDPageContentStream content =
                            new PDPageContentStream(
                                    document,
                                    page
                            )
            ) {

                dessinerEnTete(
                        content,
                        font,
                        fontBold,
                        enseignant,
                        yearMonth
                );

                dessinerEnteteTableau(
                        content,
                        fontBold,
                        y
                );

                y -= TABLE_HEADER_HEIGHT;

                float espaceDisponible =
                        y - MARGIN - 55;

                int lignesDisponibles =
                        Math.max(
                                1,
                                (int) Math.floor(
                                        espaceDisponible
                                                / ROW_HEIGHT
                                )
                        );

                int finPage =
                        Math.min(
                                index + lignesDisponibles,
                                totalSeances
                        );

                for (
                        int i = index;
                        i < finPage;
                        i++
                ) {

                    dessinerLigne(
                            content,
                            font,
                            y,
                            seances.get(i)
                    );

                    y -= ROW_HEIGHT;
                }

                boolean dernierePage =
                        finPage >= totalSeances;

                if (dernierePage) {

                    y -= 15;

                    dessinerTotaux(
                            content,
                            font,
                            fontBold,
                            y,
                            totalSeances,
                            totalEmargees,
                            totalMinutesEmarges,
                            tauxHoraire,
                            montantTotal
                    );

                    dessinerSignatures(
                            content,
                            font,
                            fontBold,
                            y - 100
                    );
                }

                index = finPage;
            }
        }
    }
    /*
     * Cette méthode est volontairement simple :
     * on recalcule le nombre de lignes possible
     * pour avancer dans la pagination.
     */
    private int finPagePourPage(
            int index,
            int totalSeances,
            PDPage page
    ) {

        float y =
                PAGE.getHeight()
                        - MARGIN
                        - HEADER_HEIGHT
                        - TABLE_HEADER_HEIGHT;

        float espaceDisponible =
                y - MARGIN - 55;

        int lignesDisponibles =
                Math.max(
                        1,
                        (int)
                                Math.floor(
                                        espaceDisponible
                                                / ROW_HEIGHT
                                )
                );

        return Math.min(
                index + lignesDisponibles,
                totalSeances
        );
    }

    // =========================================================
    // EN-TÊTE
    // =========================================================

    private void dessinerEnTete(
            PDPageContentStream content,
            PDFont font,
            PDFont fontBold,
            Enseignant enseignant,
            YearMonth yearMonth
    ) throws IOException {

        float pageWidth =
                PAGE.getWidth();

        float y =
                PAGE.getHeight()
                        - MARGIN;

        texte(
                content,
                fontBold,
                15,
                MARGIN,
                y,
                "FICHE MENSUELLE D'ÉMARGEMENT"
        );

        texteCentre(
                content,
                fontBold,
                14,
                pageWidth / 2,
                y,
                moisFrancais(yearMonth)
        );

        y -= 25;

        texte(
                content,
                fontBold,
                9,
                MARGIN,
                y,
                "ENSEIGNANT :"
        );

        texte(
                content,
                font,
                9,
                MARGIN + 70,
                y,
                nettoyer(
                        enseignant.getPrenom()
                                + " "
                                + enseignant.getNom()
                )
        );

        texte(
                content,
                fontBold,
                9,
                300,
                y,
                "MATRICULE :"
        );

        texte(
                content,
                font,
                9,
                370,
                y,
                enseignant.getMatricule() != null
                        ? enseignant.getMatricule()
                        : "-"
        );

        texte(
                content,
                fontBold,
                9,
                520,
                y,
                "TAUX HORAIRE :"
        );

        texte(
                content,
                font,
                9,
                605,
                y,
                formatMontant(
                        enseignant.getTauxHoraire() != null
                                ? enseignant.getTauxHoraire()
                                : 0
                ) + " FCFA"
        );

        y -= 20;

        texte(
                content,
                font,
                8,
                MARGIN,
                y,
                "Chaque enseignant doit signer après chaque séance effectuée."
        );

        texte(
                content,
                font,
                8,
                430,
                y,
                "Les séances non émargées ne sont pas prises en compte dans le paiement."
        );
    }

    // =========================================================
    // TABLEAU
    // =========================================================

    private void dessinerEnteteTableau(
            PDPageContentStream content,
            PDFont fontBold,
            float y
    ) throws IOException {

        float x = MARGIN;

        float[] largeurs = largeursColonnes();

        dessinerRectangle(
                content,
                x,
                y - TABLE_HEADER_HEIGHT,
                largeurTableau(),
                TABLE_HEADER_HEIGHT
        );

        String[] titres = {
                "DATE",
                "JOUR",
                "CLASSE",
                "MATIÈRE",
                "DÉBUT",
                "FIN",
                "DURÉE",
                "ÉMARGEMENT"
        };

        for (int i = 0; i < titres.length; i++) {

            dessinerCellule(
                    content,
                    fontBold,
                    titres[i],
                    x,
                    y,
                    largeurs[i],
                    TABLE_HEADER_HEIGHT,
                    7
            );

            x += largeurs[i];
        }
    }

    private void dessinerLigne(
            PDPageContentStream content,
            PDFont font,
            float y,
            SeanceMensuelle seance
    ) throws IOException {

        float x = MARGIN;

        float[] largeurs = largeursColonnes();

        String statut;

        if (seance.emargee()) {

            statut =
                    seance.present()
                            ? "ÉMARGÉ"
                            : "ABSENT";

        } else {

            statut = "À SIGNER";
        }

        String[] valeurs = {

                seance.date()
                        .format(DATE_FORMAT),

                seance.jour(),

                seance.classe(),

                seance.matiere(),

                formaterHeure(
                        seance.heureDebut()
                ),

                formaterHeure(
                        seance.heureFin()
                ),

                formaterDuree(
                        seance.duree()
                ),

                statut
        };

        for (int i = 0; i < valeurs.length; i++) {

            dessinerCellule(
                    content,
                    font,
                    valeurs[i],
                    x,
                    y,
                    largeurs[i],
                    ROW_HEIGHT,
                    7
            );

            x += largeurs[i];
        }
    }

    private float[] largeursColonnes() {

        return new float[]{
                65,   // date
                65,   // jour
                105,  // classe
                120,  // matière
                50,   // début
                50,   // fin
                55,   // durée
                252   // signature / émargement
        };
    }

    private float largeurTableau() {

        float total = 0;

        for (float largeur : largeursColonnes()) {
            total += largeur;
        }

        return total;
    }

    // =========================================================
    // TOTAUX
    // =========================================================

    private void dessinerTotaux(
            PDPageContentStream content,
            PDFont font,
            PDFont fontBold,
            float y,
            int totalSeances,
            long totalEmargees,
            int totalMinutes,
            double tauxHoraire,
            double montantTotal
    ) throws IOException {

        texte(
                content,
                fontBold,
                9,
                MARGIN,
                y,
                "RÉCAPITULATIF"
        );

        y -= 18;

        texte(
                content,
                font,
                8,
                MARGIN,
                y,
                "Séances prévues : "
                        + totalSeances
        );

        texte(
                content,
                font,
                8,
                190,
                y,
                "Séances émargées : "
                        + totalEmargees
        );

        texte(
                content,
                font,
                8,
                370,
                y,
                "Durée émargée : "
                        + formaterDuree(
                        totalMinutes
                )
        );

        texte(
                content,
                font,
                8,
                570,
                y,
                "Montant : "
                        + formatMontant(
                        montantTotal
                )
                        + " FCFA"
        );
    }

    // =========================================================
    // SIGNATURES
    // =========================================================

    private void dessinerSignatures(
            PDPageContentStream content,
            PDFont font,
            PDFont fontBold,
            float y
    ) throws IOException {

        float largeur =
                280;

        texte(
                content,
                fontBold,
                9,
                MARGIN,
                y,
                "Signature de l'enseignant"
        );

        dessinerRectangle(
                content,
                MARGIN,
                y - 65,
                largeur,
                55
        );

        texte(
                content,
                font,
                7,
                MARGIN + 10,
                y - 78,
                "Signature"
        );

        float x2 =
                PAGE.getWidth()
                        - MARGIN
                        - largeur;

        texte(
                content,
                fontBold,
                9,
                x2,
                y,
                "Visa du surveillant"
        );

        dessinerRectangle(
                content,
                x2,
                y - 65,
                largeur,
                55
        );

        texte(
                content,
                font,
                7,
                x2 + 10,
                y - 78,
                "Signature / Cachet"
        );
    }

    // =========================================================
    // PDF SANS COURS
    // =========================================================

    private void genererPdfSansCours(
            PDDocument document,
            PDFont font,
            PDFont fontBold,
            Enseignant enseignant,
            YearMonth yearMonth
    ) throws IOException {

        PDPage page =
                new PDPage(PAGE);

        document.addPage(page);

        try (
                PDPageContentStream content =
                        new PDPageContentStream(
                                document,
                                page
                        )
        ) {

            dessinerEnTete(
                    content,
                    font,
                    fontBold,
                    enseignant,
                    yearMonth
            );

            texteCentre(
                    content,
                    fontBold,
                    11,
                    PAGE.getWidth() / 2,
                    PAGE.getHeight() / 2,
                    "Aucun cours programmé pour cet enseignant durant ce mois."
            );
        }
    }

    // =========================================================
    // UTILITAIRES
    // =========================================================

    private String construireCle(
            Long emploiDuTempsId,
            LocalDate date
    ) {

        return emploiDuTempsId
                + "_"
                + date;
    }

    private DayOfWeek convertirJour(
            String jour
    ) {

        if (jour == null) {
            return null;
        }

        String valeur =
                jour.trim()
                        .toUpperCase(Locale.ROOT)
                        .replace("É", "E")
                        .replace("È", "E")
                        .replace("Ê", "E");

        return switch (valeur) {

            case "LUNDI" ->
                    DayOfWeek.MONDAY;

            case "MARDI" ->
                    DayOfWeek.TUESDAY;

            case "MERCREDI" ->
                    DayOfWeek.WEDNESDAY;

            case "JEUDI" ->
                    DayOfWeek.THURSDAY;

            case "VENDREDI" ->
                    DayOfWeek.FRIDAY;

            case "SAMEDI" ->
                    DayOfWeek.SATURDAY;

            case "DIMANCHE" ->
                    DayOfWeek.SUNDAY;

            default ->
                    null;
        };
    }

    private String jourFrancais(
            LocalDate date
    ) {

        return date.getDayOfWeek()
                .getDisplayName(
                        TextStyle.FULL,
                        Locale.FRENCH
                );
    }

    private String moisFrancais(
            YearMonth yearMonth
    ) {

        String texte =
                yearMonth
                        .atDay(1)
                        .getMonth()
                        .getDisplayName(
                                TextStyle.FULL,
                                Locale.FRENCH
                        );

        return texte.substring(0, 1)
                .toUpperCase(Locale.FRENCH)
                + texte.substring(1)
                + " "
                + yearMonth.getYear();
    }

    private String formaterHeure(
            int minutes
    ) {

        int heures =
                minutes / 60;

        int minutesRestantes =
                minutes % 60;

        return String.format(
                Locale.FRANCE,
                "%02d:%02d",
                heures,
                minutesRestantes
        );
    }

    private String formaterDuree(
            int minutes
    ) {

        int heures =
                minutes / 60;

        int minutesRestantes =
                minutes % 60;

        if (heures > 0 && minutesRestantes > 0) {
            return heures
                    + "h"
                    + minutesRestantes;
        }

        if (heures > 0) {
            return heures + "h";
        }

        return minutesRestantes + " min";
    }

    private String formatMontant(
            double montant
    ) {

        return String.format(
                Locale.FRANCE,
                "%,.0f",
                montant
        ).replace(',', ' ');
    }

    private String nettoyer(
            String texte
    ) {

        if (texte == null) {
            return "";
        }

        return texte
                .replace("’", "'")
                .replace("–", "-")
                .replace("—", "-");
    }

    private PDFont chargerPolice(
            PDDocument document,
            String chemin
    ) throws IOException {

        InputStream input =
                getClass()
                        .getResourceAsStream(
                                chemin
                        );

        if (input == null) {

            throw new IOException(
                    "Police introuvable : "
                            + chemin
            );
        }

        return PDType0Font.load(
                document,
                input
        );
    }

    // =========================================================
    // DESSIN PDF
    // =========================================================

    private void dessinerCellule(
            PDPageContentStream content,
            PDFont font,
            String texte,
            float x,
            float y,
            float largeur,
            float hauteur,
            float taille
    ) throws IOException {

        content.addRect(
                x,
                y - hauteur,
                largeur,
                hauteur
        );

        content.stroke();

        String valeur =
                texte != null
                        ? nettoyer(texte)
                        : "";

        valeur =
                tronquer(
                        valeur,
                        font,
                        taille,
                        largeur - 8
                );

        float largeurTexte =
                font.getStringWidth(valeur)
                        / 1000
                        * taille;

        float textX =
                x
                        + Math.max(
                        4,
                        (largeur - largeurTexte) / 2
                );

        float textY =
                y
                        - hauteur / 2
                        - taille / 3;

        content.beginText();

        content.setFont(
                font,
                taille
        );

        content.newLineAtOffset(
                textX,
                textY
        );

        content.showText(
                valeur
        );

        content.endText();
    }

    private void dessinerRectangle(
            PDPageContentStream content,
            float x,
            float y,
            float largeur,
            float hauteur
    ) throws IOException {

        content.addRect(
                x,
                y,
                largeur,
                hauteur
        );

        content.stroke();
    }

    private void texte(
            PDPageContentStream content,
            PDFont font,
            float taille,
            float x,
            float y,
            String texte
    ) throws IOException {

        content.beginText();

        content.setFont(
                font,
                taille
        );

        content.newLineAtOffset(
                x,
                y
        );

        content.showText(
                nettoyer(texte)
        );

        content.endText();
    }

    private void texteCentre(
            PDPageContentStream content,
            PDFont font,
            float taille,
            float centreX,
            float y,
            String texte
    ) throws IOException {

        String valeur =
                nettoyer(texte);

        float largeur =
                font.getStringWidth(valeur)
                        / 1000
                        * taille;

        texte(
                content,
                font,
                taille,
                centreX - largeur / 2,
                y,
                valeur
        );
    }

    private String tronquer(
            String texte,
            PDFont font,
            float taille,
            float largeurMax
    ) throws IOException {

        if (texte == null) {
            return "";
        }

        if (
                font.getStringWidth(texte)
                        / 1000
                        * taille
                        <= largeurMax
        ) {
            return texte;
        }

        String suffixe = "...";

        String resultat = texte;

        while (
                resultat.length() > 0
                        && font.getStringWidth(
                        resultat + suffixe
                ) / 1000 * taille
                        > largeurMax
        ) {

            resultat =
                    resultat.substring(
                            0,
                            resultat.length() - 1
                    );
        }

        return resultat + suffixe;
    }

    // =========================================================
    // DTO INTERNE
    // =========================================================

    private record SeanceMensuelle(
            LocalDate date,
            String jour,
            String classe,
            String matiere,
            int heureDebut,
            int heureFin,
            int duree,
            boolean emargee,
            boolean present
    ) {
    }
}