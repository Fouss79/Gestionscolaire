package com.saas.school.service;

import com.saas.school.entity.*;
import com.saas.school.repository.InscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FicheRenseignementService {

    private final InscriptionRepository inscriptionRepository;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private static final float MARGE_GAUCHE = 50;
    private static final float PHOTO_TAILLE = 90;

    /**
     * Génère la fiche de renseignement à partir d'une INSCRIPTION (pas
     * directement de l'Eleve) — cohérent avec le reste de l'app où les
     * écrans élève (paiements, etc.) manipulent des inscriptionId. La
     * classe/année scolaire affichées viennent de l'inscription, qui
     * reflète la scolarité actuelle, plutôt que des champs "classe"/
     * "anneeScolaire" bruts sur Eleve qui peuvent être obsolètes après
     * un changement de classe ou une réinscription.
     */
    public byte[] genererFichePdf(Long inscriptionId) {

        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        Eleve eleve = inscription.getEleve();

        if (eleve == null) {
            throw new RuntimeException("Élève introuvable pour cette inscription");
        }

        Ecole ecole = inscription.getEcole() != null ? inscription.getEcole() : eleve.getEcole();
        Classe classe = inscription.getClasse();

        try (PDDocument document = new PDDocument()) {

            // ===== POLICES UNICODE =====
            PDFont fontRegular;
            PDFont fontBold;

            try (InputStream regularStream = new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream();
                 InputStream boldStream = new ClassPathResource("fonts/DejaVuSans-Bold.ttf").getInputStream()) {

                fontRegular = PDType0Font.load(document, regularStream);
                fontBold = PDType0Font.load(document, boldStream);
            }

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float largeurPage = PDRectangle.A4.getWidth();
            float y = PDRectangle.A4.getHeight() - 55;

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {

                // ===== EN-TÊTE ÉCOLE =====
                cs.beginText();
                cs.setFont(fontBold, 16);
                cs.newLineAtOffset(MARGE_GAUCHE, y);
                cs.showText(nettoyerTexte(ecole != null && ecole.getNom() != null ? ecole.getNom() : "École"));
                cs.endText();

                y -= 28;

                cs.beginText();
                cs.setFont(fontBold, 14);
                cs.newLineAtOffset(MARGE_GAUCHE, y);
                cs.showText("FICHE DE RENSEIGNEMENT ÉLÈVE");
                cs.endText();

                y -= 15;

                cs.setStrokingColor(0.78f, 0.78f, 0.78f);
                cs.moveTo(MARGE_GAUCHE, y);
                cs.lineTo(largeurPage - MARGE_GAUCHE, y);
                cs.stroke();

                // ===== PHOTO (en haut à droite, si disponible) =====
                float yDebutBloc = y - 20;

                if (eleve.getPhotoUrl() != null && !eleve.getPhotoUrl().isBlank()) {
                    try {
                        PDImageXObject image = chargerImage(document, eleve.getPhotoUrl());
                        if (image != null) {
                            cs.drawImage(
                                    image,
                                    largeurPage - MARGE_GAUCHE - PHOTO_TAILLE,
                                    yDebutBloc - PHOTO_TAILLE,
                                    PHOTO_TAILLE,
                                    PHOTO_TAILLE
                            );
                        }
                    } catch (Exception ignored) {
                        // Photo indisponible/illisible : on continue sans bloquer la génération
                    }
                }

                y = yDebutBloc;

                // ===== IDENTITÉ DE L'ÉLÈVE =====
                y = ecrireSection(cs, fontBold, y, "Identité de l'élève");

                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Nom",
                        eleve.getNom());
                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Prénom",
                        eleve.getPrenom());
                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Matricule",
                        eleve.getMatricule());
                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Date de naissance",
                        eleve.getDateNaissance() != null ? eleve.getDateNaissance().format(DATE_FORMAT) : null);
                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Lieu de naissance",
                        eleve.getLieuNaissance());
                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Sexe",
                        eleve.getSexe());
                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Nationalité",
                        eleve.getNationalite());
                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "N° extrait de naissance",
                        eleve.getNumeroExtraitNaissance());

                y -= 10;

                // ===== INFORMATIONS MÉDICALES =====
                if (notBlank(eleve.getGroupeSanguin()) || notBlank(eleve.getAllergiesMaladies())) {

                    y = ecrireSection(cs, fontBold, y, "Informations médicales");

                    y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Groupe sanguin",
                            eleve.getGroupeSanguin());
                    y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Allergies / maladies",
                            eleve.getAllergiesMaladies());

                    y -= 10;
                }

                // ===== COORDONNÉES DE L'ÉLÈVE =====
                y = ecrireSection(cs, fontBold, y, "Coordonnées");

                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Adresse",
                        eleve.getAdresse());
                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Téléphone",
                        eleve.getTelephone());
                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Email",
                        eleve.getEmail());

                y -= 10;

                // ===== SCOLARITÉ =====
                y = ecrireSection(cs, fontBold, y, "Scolarité");

                y = ecrireLigne(
                        cs,
                        fontRegular,
                        fontBold,
                        MARGE_GAUCHE,
                        y,
                        "Classe",
                        classe != null ? classe.getNomComplet() : null
                );

                y = ecrireLigne(
                        cs,
                        fontRegular,
                        fontBold,
                        MARGE_GAUCHE,
                        y,
                        "Année scolaire",
                        inscription.getAnneeScolaire() != null
                                ? inscription.getAnneeScolaire().getNom()
                                : null
                );

                y = ecrireLigne(
                        cs,
                        fontRegular,
                        fontBold,
                        MARGE_GAUCHE,
                        y,
                        "Statut",
                        inscription.getStatut() != null
                                ? inscription.getStatut().name()
                                : libelleStatut(eleve.getStatut())
                );

// Décision administrative liée à la réinscription
                if (inscription.getDecision() != null) {

                    String decision = switch (inscription.getDecision()) {
                        case ADMIS -> "Admis";
                        case REDOUBLANT -> "Redoublant";
                    };

                    y = ecrireLigne(
                            cs,
                            fontRegular,
                            fontBold,
                            MARGE_GAUCHE,
                            y,
                            "Décision",
                            decision
                    );

                    // Indicateur explicite
                    if (inscription.getDecision() == DecisionScolaire.REDOUBLANT) {
                        y = ecrireLigne(
                                cs,
                                fontRegular,
                                fontBold,
                                MARGE_GAUCHE,
                                y,
                                "Redoublant",
                                "Oui"
                        );
                    }
                }

                y = ecrireLigne(
                        cs,
                        fontRegular,
                        fontBold,
                        MARGE_GAUCHE,
                        y,
                        "Date d'inscription",
                        inscription.getCreatedAt() != null
                                ? inscription.getCreatedAt().format(DATE_FORMAT)
                                : (
                                eleve.getDateInscription() != null
                                        ? eleve.getDateInscription().format(DATE_FORMAT)
                                        : null
                        )
                );
                y -= 10;

                // ===== PARENT / TUTEUR =====
                y = ecrireSection(cs, fontBold, y, "Parent / Tuteur");

                String nomTuteurComplet = ((eleve.getPrenomTuteur() != null ? eleve.getPrenomTuteur() : "") + " "
                        + (eleve.getNomTuteur() != null ? eleve.getNomTuteur() : "")).trim();

                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Nom complet",
                        !nomTuteurComplet.isBlank() ? nomTuteurComplet : null);
                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Lien de parenté",
                        eleve.getLienParente());
                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Téléphone",
                        eleve.getTelephoneTuteur());
                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Email",
                        eleve.getEmailTuteur());
                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Profession",
                        eleve.getProfessionTuteur());
                y = ecrireLigne(cs, fontRegular, fontBold, MARGE_GAUCHE, y, "Adresse",
                        eleve.getAdresseTuteur());

                y -= 35;

                // ===== ZONE SIGNATURE =====
                float largeurSignature = (largeurPage - 2 * MARGE_GAUCHE - 20) / 2;

                cs.setStrokingColor(0.7f, 0.7f, 0.7f);
                cs.moveTo(MARGE_GAUCHE, y);
                cs.lineTo(MARGE_GAUCHE + largeurSignature, y);
                cs.stroke();

                cs.moveTo(MARGE_GAUCHE + largeurSignature + 20, y);
                cs.lineTo(MARGE_GAUCHE + 2 * largeurSignature + 20, y);
                cs.stroke();

                y -= 15;

                cs.beginText();
                cs.setFont(fontRegular, 9);
                cs.newLineAtOffset(MARGE_GAUCHE, y);
                cs.showText("Signature du parent / tuteur");
                cs.endText();

                cs.beginText();
                cs.setFont(fontRegular, 9);
                cs.newLineAtOffset(MARGE_GAUCHE + largeurSignature + 20, y);
                cs.showText("Signature / cachet de l'établissement");
                cs.endText();

                // ===== PIED DE PAGE =====
                cs.beginText();
                cs.setFont(fontRegular, 8);
                cs.newLineAtOffset(MARGE_GAUCHE, 40);
                cs.showText("Document généré automatiquement — fiche de renseignement de l'élève.");
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération de la fiche de renseignement", e);
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private PDImageXObject chargerImage(PDDocument document, String photoUrl) throws IOException {

        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
            URL url = URI.create(photoUrl).toURL();
            try (InputStream in = url.openStream()) {
                return PDImageXObject.createFromByteArray(document, in.readAllBytes(), photoUrl);
            }
        }

        // Chemin local (ex: fichier stocké sur le serveur)
        try (InputStream in = new ClassPathResource(photoUrl).getInputStream()) {
            return PDImageXObject.createFromByteArray(document, in.readAllBytes(), photoUrl);
        }
    }

    private float ecrireSection(PDPageContentStream cs, PDFont fontBold, float y, String titre) throws IOException {

        cs.beginText();
        cs.setFont(fontBold, 11);
        cs.newLineAtOffset(MARGE_GAUCHE, y);
        cs.showText(nettoyerTexte(titre));
        cs.endText();

        return y - 20;
    }

    /**
     * N'écrit la ligne que si une valeur existe — évite d'afficher des
     * champs vides ("Email : -") sur un document administratif où
     * l'absence d'info doit rester discrète plutôt qu'affichée en tiret.
     */
    private float ecrireLigne(
            PDPageContentStream cs, PDFont fontRegular, PDFont fontBold,
            float x, float y, String label, String valeur
    ) throws IOException {

        if (!notBlank(valeur)) {
            return y;
        }

        cs.beginText();
        cs.setFont(fontRegular, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(nettoyerTexte(label) + " :");
        cs.endText();

        cs.beginText();
        cs.setFont(fontBold, 10);
        cs.newLineAtOffset(x + 170, y);
        cs.showText(nettoyerTexte(valeur));
        cs.endText();

        return y - 18;
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String nettoyerTexte(String texte) {
        if (texte == null) return "";
        return texte
                .replace('\u202F', ' ')
                .replace('\u00A0', ' ')
                .replace('\u2007', ' ')
                .replace('\u2009', ' ');
    }

    private String libelleStatut(Eleve.StatutEleve statut) {
        if (statut == null) return null;

        return switch (statut) {
            case NOUVEAU -> "Nouveau";
            case REDOUBLANT -> "Redoublant";
            case TRANSFERE -> "Transféré";
            case DIPLOME -> "Diplômé";
            case ABANDON -> "Abandon";
        };
    }
    }
