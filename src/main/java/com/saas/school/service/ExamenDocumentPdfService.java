
        package com.saas.school.service;

import com.saas.school.entity.*;
import com.saas.school.repository.EpreuveExamenRepository;
import com.saas.school.repository.ExamenRepository;
import com.saas.school.repository.RepartitionExamenRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamenDocumentPdfService {

    private final ExamenRepository examenRepository;
    private final EpreuveExamenRepository epreuveExamenRepository;
    private final RepartitionExamenRepository repartitionExamenRepository;

    private static final float MARGIN = 40;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();

    private static final float TABLE_TOP = 610;
    private static final float ROW_HEIGHT = 22;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ============================================================
    // 1. LISTES DES SALLES
    // ============================================================

    public byte[] genererListesSallesPdf(Long examenId) throws IOException {

        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() ->
                        new RuntimeException("Examen introuvable : " + examenId));

        List<RepartitionExamen> repartitions =
                repartitionExamenRepository.findByExamenId(examenId);

        if (repartitions.isEmpty()) {
            throw new RuntimeException(
                    "Aucune répartition n'a été effectuée pour cet examen."
            );
        }

        String nomEtablissement = getNomEtablissement(examen);

        // Groupe uniquement par salle
        Map<Long, List<RepartitionExamen>> parSalle =
                repartitions.stream()
                        .collect(Collectors.groupingBy(
                                r -> r.getSalle().getId(),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDType0Font font = chargerPolice(document);
            PDType0Font fontBold = chargerPoliceGras(document);

            int numeroPage = 0;

            for (List<RepartitionExamen> elevesSalle : parSalle.values()) {

                elevesSalle.sort(
                        Comparator.comparing(
                                r -> nomEleve(r.getInscription()),
                                String.CASE_INSENSITIVE_ORDER
                        )
                );

                Salle salle = elevesSalle.get(0).getSalle();

                int index = 0;

                while (index < elevesSalle.size()) {

                    PDPage page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    numeroPage++;

                    try (PDPageContentStream content =
                                 new PDPageContentStream(document, page)) {

                        dessinerEnteteListeSalle(
                                content,
                                examen,
                                nomEtablissement,
                                salle,
                                font,
                                fontBold
                        );

                        float y = TABLE_TOP;

                        y = dessinerEnteteTableauSalle(
                                content,
                                y,
                                fontBold
                        );

                        while (index < elevesSalle.size()
                                && y - ROW_HEIGHT > 60) {

                            RepartitionExamen repartition =
                                    elevesSalle.get(index);

                            dessinerLigneSalle(
                                    content,
                                    repartition,
                                    index + 1,
                                    y,
                                    font
                            );

                            y -= ROW_HEIGHT;
                            index++;
                        }

                        dessinerNumeroPage(
                                content,
                                numeroPage,
                                font
                        );
                    }
                }
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    // ============================================================
    // 2. FEUILLES DE PRÉSENCE
    //    ÉPREUVE + SALLE + CLASSE
    // ============================================================

    public byte[] genererFeuillePresencePdf(
            Long examenId,
            Long epreuveId
    ) throws IOException {

        Examen examen = examenRepository.findById(examenId)
                .orElseThrow(() ->
                        new RuntimeException("Examen introuvable : " + examenId));

        EpreuveExamen epreuve = epreuveExamenRepository.findById(epreuveId)
                .orElseThrow(() ->
                        new RuntimeException("Épreuve introuvable : " + epreuveId));

        // Vérification : l'épreuve appartient bien à cet examen
        if (epreuve.getExamen() == null
                || !Objects.equals(
                epreuve.getExamen().getId(),
                examenId
        )) {
            throw new RuntimeException(
                    "Cette épreuve n'appartient pas à cet examen."
            );
        }

        List<RepartitionExamen> repartitions =
                repartitionExamenRepository.findByExamenId(examenId);

        if (repartitions.isEmpty()) {
            throw new RuntimeException(
                    "Aucune répartition n'a été effectuée pour cet examen."
            );
        }

        String nomEtablissement = getNomEtablissement(examen);

        /*
         * IMPORTANT :
         *
         * Les salles sont fixes pour l'examen.
         * On utilise donc RepartitionExamen comme source unique.
         *
         * Pour la feuille de présence, on groupe par :
         *
         *      SALLE + CLASSE
         *
         * L'épreuve ne change pas cette répartition.
         */

        Map<String, List<RepartitionExamen>> groupes =
                repartitions.stream()
                        .collect(Collectors.groupingBy(
                                r -> construireCleSalleClasse(r),
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDType0Font font = chargerPolice(document);
            PDType0Font fontBold = chargerPoliceGras(document);

            int numeroPage = 0;

            for (List<RepartitionExamen> groupe : groupes.values()) {

                if (groupe.isEmpty()) {
                    continue;
                }

                groupe.sort(
                        Comparator.comparing(
                                r -> nomEleve(r.getInscription()),
                                String.CASE_INSENSITIVE_ORDER
                        )
                );

                Salle salle = groupe.get(0).getSalle();

                Classe classe = getClasse(
                        groupe.get(0).getInscription()
                );

                int index = 0;

                while (index < groupe.size()) {

                    PDPage page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    numeroPage++;

                    try (PDPageContentStream content =
                                 new PDPageContentStream(document, page)) {

                        dessinerEntetePresence(
                                content,
                                examen,
                                epreuve,
                                nomEtablissement,
                                salle,
                                classe,
                                font,
                                fontBold
                        );

                        float y = TABLE_TOP;

                        y = dessinerEnteteTableauPresence(
                                content,
                                y,
                                fontBold
                        );

                        int numeroEleve = index + 1;

                        while (index < groupe.size()
                                && y - ROW_HEIGHT > 60) {

                            RepartitionExamen repartition =
                                    groupe.get(index);

                            dessinerLignePresence(
                                    content,
                                    repartition,
                                    numeroEleve,
                                    y,
                                    font
                            );

                            y -= ROW_HEIGHT;
                            index++;
                            numeroEleve++;
                        }

                        dessinerNumeroPage(
                                content,
                                numeroPage,
                                font
                        );
                    }
                }
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    // ============================================================
    // ENTÊTE LISTE SALLE
    // ============================================================

    private void dessinerEnteteListeSalle(
            PDPageContentStream content,
            Examen examen,
            String nomEtablissement,
            Salle salle,
            PDType0Font font,
            PDType0Font fontBold
    ) throws IOException {

        float y = 770;

        texteCentre(
                content,
                nomEtablissement,
                y,
                16,
                fontBold
        );

        y -= 25;

        texteCentre(
                content,
                "LISTE DES ÉLÈVES PAR SALLE",
                y,
                13,
                fontBold
        );

        y -= 25;

        texteCentre(
                content,
                "EXAMEN : " + valeur(examen.getNom()),
                y,
                11,
                font
        );

        y -= 20;

        texteCentre(
                content,
                "SALLE : " + valeur(salle.getNom()),
                y,
                12,
                fontBold
        );

        y -= 18;

        if (examen.getDateDebut() != null) {

            texteCentre(
                    content,
                    "Du " + examen.getDateDebut().format(DATE_FORMAT)
                            + (
                            examen.getDateFin() != null
                                    ? " au " + examen.getDateFin().format(DATE_FORMAT)
                                    : ""
                    ),
                    y,
                    9,
                    font
            );
        }
    }

    // ============================================================
    // ENTÊTE FEUILLE DE PRÉSENCE
    // ============================================================

    private void dessinerEntetePresence(
            PDPageContentStream content,
            Examen examen,
            EpreuveExamen epreuve,
            String nomEtablissement,
            Salle salle,
            Classe classe,
            PDType0Font font,
            PDType0Font fontBold
    ) throws IOException {

        float y = 770;

        // NOM DE L'ÉTABLISSEMENT
        texteCentre(
                content,
                nomEtablissement,
                y,
                16,
                fontBold
        );

        y -= 25;

        texteCentre(
                content,
                "FEUILLE DE PRÉSENCE",
                y,
                14,
                fontBold
        );

        y -= 24;

        texteCentre(
                content,
                "EXAMEN : " + valeur(examen.getNom()),
                y,
                11,
                font
        );

        y -= 20;

        // ÉPREUVE
        String nomMatiere = "Épreuve";

        if (epreuve.getCoefficientMatiere() != null
                && epreuve.getCoefficientMatiere().getMatiere() != null) {

            nomMatiere =
                    valeur(epreuve
                            .getCoefficientMatiere()
                            .getMatiere()
                            .getNom());
        }

        texteCentre(
                content,
                "ÉPREUVE : " + nomMatiere,
                y,
                12,
                fontBold
        );

        y -= 20;

        // SALLE + CLASSE
        String salleTexte =
                "SALLE : " + valeur(salle.getNom());

        String classeTexte =
                "CLASSE : " + nomClasse(classe);

        texteCentre(
                content,
                salleTexte + "     |     " + classeTexte,
                y,
                11,
                fontBold
        );

        y -= 20;

        // DATE / HORAIRE
        if (epreuve.getCreneau() != null) {

            String creneauTexte = construireTexteCreneau(epreuve);

            if (!creneauTexte.isBlank()) {

                texteCentre(
                        content,
                        creneauTexte,
                        y,
                        9,
                        font
                );

                y -= 18;
            }
        }

        if (epreuve.getDureeMinutes() != null) {

            texteCentre(
                    content,
                    "Durée : " + epreuve.getDureeMinutes() + " minutes",
                    y,
                    9,
                    font
            );
        }
    }

    // ============================================================
    // TABLEAU LISTE SALLE
    // ============================================================

    private float dessinerEnteteTableauSalle(
            PDPageContentStream content,
            float y,
            PDType0Font fontBold
    ) throws IOException {

        float x = MARGIN;

        float[] largeurs = {
                35,
                220,
                100,
                140
        };

        String[] titres = {
                "N°",
                "ÉLÈVE",
                "CLASSE",
                "MATRICULE"
        };

        dessinerCellulesEntete(
                content,
                x,
                y,
                largeurs,
                titres,
                fontBold
        );

        return y - ROW_HEIGHT;
    }

    private void dessinerLigneSalle(
            PDPageContentStream content,
            RepartitionExamen repartition,
            int numero,
            float y,
            PDType0Font font
    ) throws IOException {

        float x = MARGIN;

        float[] largeurs = {
                35,
                220,
                100,
                140
        };

        String[] valeurs = {
                String.valueOf(numero),
                nomEleve(repartition.getInscription()),
                nomClasse(getClasse(repartition.getInscription())),
                matricule(repartition.getInscription())
        };

        dessinerLigne(
                content,
                x,
                y,
                largeurs,
                valeurs,
                font
        );
    }

    // ============================================================
    // TABLEAU PRÉSENCE
    // ============================================================

    private float dessinerEnteteTableauPresence(
            PDPageContentStream content,
            float y,
            PDType0Font fontBold
    ) throws IOException {

        float x = MARGIN;

        float[] largeurs = {
                35,
                205,
                90,
                100,
                50,
                60
        };

        String[] titres = {
                "N°",
                "ÉLÈVE",
                "MATRICULE",
                "STATUT",
                "PRÉS.",
                "SIGNATURE"
        };

        dessinerCellulesEntete(
                content,
                x,
                y,
                largeurs,
                titres,
                fontBold
        );

        return y - ROW_HEIGHT;
    }

    private void dessinerLignePresence(
            PDPageContentStream content,
            RepartitionExamen repartition,
            int numero,
            float y,
            PDType0Font font
    ) throws IOException {

        float x = MARGIN;

        float[] largeurs = {
                35,
                205,
                90,
                100,
                50,
                60
        };

        String statut = "NON CONFIRME";

        /*
         * La répartition ne contient pas le statut de composition.
         * Le statut pourra être ajouté ici si tu souhaites charger
         * CompositionEpreuve dans le service.
         *
         * Pour l'instant, la feuille laisse le statut vide/manuel.
         */

        String[] valeurs = {
                String.valueOf(numero),
                nomEleve(repartition.getInscription()),
                matricule(repartition.getInscription()),
                statut,
                "",
                ""
        };

        dessinerLigne(
                content,
                x,
                y,
                largeurs,
                valeurs,
                font
        );
    }

    // ============================================================
    // DESSIN TABLEAU
    // ============================================================

    private void dessinerCellulesEntete(
            PDPageContentStream content,
            float x,
            float y,
            float[] largeurs,
            String[] titres,
            PDType0Font font
    ) throws IOException {

        float currentX = x;

        for (int i = 0; i < titres.length; i++) {

            dessinerRectangle(
                    content,
                    currentX,
                    y - ROW_HEIGHT,
                    largeurs[i],
                    ROW_HEIGHT
            );

            texteDansCellule(
                    content,
                    titres[i],
                    currentX,
                    y - ROW_HEIGHT,
                    largeurs[i],
                    ROW_HEIGHT,
                    font,
                    8
            );

            currentX += largeurs[i];
        }
    }

    private void dessinerLigne(
            PDPageContentStream content,
            float x,
            float y,
            float[] largeurs,
            String[] valeurs,
            PDType0Font font
    ) throws IOException {

        float currentX = x;

        for (int i = 0; i < valeurs.length; i++) {

            dessinerRectangle(
                    content,
                    currentX,
                    y - ROW_HEIGHT,
                    largeurs[i],
                    ROW_HEIGHT
            );

            texteDansCellule(
                    content,
                    valeurs[i],
                    currentX,
                    y - ROW_HEIGHT,
                    largeurs[i],
                    ROW_HEIGHT,
                    font,
                    8
            );

            currentX += largeurs[i];
        }
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

    private void texteDansCellule(
            PDPageContentStream content,
            String texte,
            float x,
            float y,
            float largeur,
            float hauteur,
            PDType0Font font,
            float taille
    ) throws IOException {

        if (texte == null) {
            texte = "";
        }

        // Évite que le texte dépasse de la cellule
        texte = tronquerTexte(
                texte,
                font,
                taille,
                largeur - 8
        );

        float largeurTexte =
                font.getStringWidth(texte) / 1000 * taille;

        float texteX =
                x + Math.max(4, (largeur - largeurTexte) / 2);

        float texteY =
                y + (hauteur - taille) / 2 + 3;

        content.beginText();
        content.setFont(font, taille);
        content.newLineAtOffset(texteX, texteY);
        content.showText(texte);
        content.endText();
    }

    // ============================================================
    // NUMÉRO DE PAGE
    // ============================================================

    private void dessinerNumeroPage(
            PDPageContentStream content,
            int numeroPage,
            PDType0Font font
    ) throws IOException {

        String texte = "Page " + numeroPage;

        float taille = 8;

        float largeur =
                font.getStringWidth(texte) / 1000 * taille;

        float x =
                (PAGE_WIDTH - largeur) / 2;

        content.beginText();
        content.setFont(font, taille);
        content.newLineAtOffset(x, 25);
        content.showText(texte);
        content.endText();
    }

    // ============================================================
    // TEXTE CENTRÉ
    // ============================================================

    private void texteCentre(
            PDPageContentStream content,
            String texte,
            float y,
            float taille,
            PDType0Font font
    ) throws IOException {

        if (texte == null) {
            return;
        }

        float largeur =
                font.getStringWidth(texte) / 1000 * taille;

        float x =
                (PAGE_WIDTH - largeur) / 2;

        content.beginText();
        content.setFont(font, taille);
        content.newLineAtOffset(x, y);
        content.showText(texte);
        content.endText();
    }

    // ============================================================
    // POLICES
    // ============================================================

    private PDType0Font chargerPolice(
            PDDocument document
    ) throws IOException {

        InputStream inputStream =
                getClass()
                        .getResourceAsStream(
                                "/fonts/DejaVuSans.ttf"
                        );

        if (inputStream == null) {
            throw new IOException(
                    "Police introuvable : " +
                            "src/main/resources/fonts/DejaVuSans.ttf"
            );
        }

        return PDType0Font.load(
                document,
                inputStream
        );
    }

    private PDType0Font chargerPoliceGras(
            PDDocument document
    ) throws IOException {

        InputStream inputStream =
                getClass()
                        .getResourceAsStream(
                                "/fonts/DejaVuSans-Bold.ttf"
                        );

        // Si la police Bold n'existe pas,
        // on utilise la police normale.
        if (inputStream == null) {
            return chargerPolice(document);
        }

        return PDType0Font.load(
                document,
                inputStream
        );
    }

    // ============================================================
    // UTILITAIRES
    // ============================================================

    private String getNomEtablissement(
            Examen examen
    ) {

        if (examen.getEcole() == null) {
            return "ÉTABLISSEMENT";
        }

        if (examen.getEcole().getNom() == null
                || examen.getEcole().getNom().isBlank()) {

            return "ÉTABLISSEMENT";
        }

        return examen.getEcole().getNom();
    }

    private String construireCleSalleClasse(
            RepartitionExamen repartition
    ) {

        Long salleId =
                repartition.getSalle() != null
                        ? repartition.getSalle().getId()
                        : 0L;

        Classe classe =
                getClasse(repartition.getInscription());

        Long classeId =
                classe != null
                        ? classe.getId()
                        : 0L;

        return salleId + "-" + classeId;
    }

    private Classe getClasse(
            Inscription inscription
    ) {

        if (inscription == null) {
            return null;
        }

        return inscription.getClasse();
    }

    private String nomClasse(
            Classe classe
    ) {

        if (classe == null) {
            return "";
        }

        return valeur(classe.getNomComplet());
    }

    private String nomEleve(
            Inscription inscription
    ) {

        if (inscription == null
                || inscription.getEleve() == null) {

            return "";
        }

        Eleve eleve = inscription.getEleve();

        String nom = valeur(eleve.getNom());
        String prenom = valeur(eleve.getPrenom());

        return (nom + " " + prenom).trim();
    }

    private String matricule(
            Inscription inscription
    ) {

        if (inscription == null
                || inscription.getEleve() == null) {

            return "";
        }

        return valeur(
                inscription.getEleve().getMatricule()
        );
    }

    private String construireTexteCreneau(
            EpreuveExamen epreuve
    ) {

        if (epreuve.getCreneau() == null) {
            return "";
        }

        CreneauExamen creneau =
                epreuve.getCreneau();

        List<String> morceaux =
                new ArrayList<>();

        /*
         * Adapter ici selon les champs exacts
         * de ton entité CreneauExamen.
         *
         * Cette partie reste volontairement prudente
         * pour ne pas dépendre de getters qui pourraient
         * être différents dans ton projet.
         */

        return String.join(" - ", morceaux);
    }

    private String valeur(
            String texte
    ) {

        return texte == null ? "" : texte;
    }

    private String tronquerTexte(
            String texte,
            PDType0Font font,
            float taille,
            float largeurMax
    ) throws IOException {

        if (texte == null) {
            return "";
        }

        if (font.getStringWidth(texte) / 1000 * taille
                <= largeurMax) {

            return texte;
        }

        String suffixe = "...";

        String resultat = texte;

        while (!resultat.isEmpty()) {

            resultat =
                    resultat.substring(
                            0,
                            resultat.length() - 1
                    );

            String candidat =
                    resultat + suffixe;

            if (font.getStringWidth(candidat) / 1000 * taille
                    <= largeurMax) {

                return candidat;
            }
        }

        return suffixe;
    }
}
