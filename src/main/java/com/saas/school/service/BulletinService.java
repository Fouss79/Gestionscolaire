
package com.saas.school.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.saas.school.dto.BulletinDTO;
import com.saas.school.entity.*;
import com.saas.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BulletinService {

    private static final String ECOLE_NOM_DEFAUT = "ETABLISSEMENT";
    private static final String ECOLE_ADRESSE_DEFAUT = "";

    private final NoteRepository noteRepository;
    private final InscriptionRepository inscriptionRepository;
    private final EvaluationConduiteRepository evaluationConduiteRepository;
    private final AffectationEnseignantRepository affectationEnseignantRepository;
    private final EcoleRepository ecoleRepository;

    // =========================================================
    // ÉTABLISSEMENT
    // =========================================================

    private Ecole resolveEcole(Inscription inscription) {
        if (inscription == null) {
            return null;
        }

        try {
            if (inscription.getClasse() != null
                    && inscription.getClasse().getEcole() != null) {
                return inscription.getClasse().getEcole();
            }
        } catch (Exception e) {
            System.out.println(
                    "Impossible de résoudre l'établissement : " + e.getMessage()
            );
        }

        return null;
    }

    // =========================================================
    // CONDUITE
    // =========================================================

    /**
     * La conduite est une évaluation indépendante des matières.
     * Elle est récupérée par inscription et période.
     */
    private EvaluationConduite trouverConduite(
            Inscription inscription,
            String periode
    ) {
        if (inscription == null
                || inscription.getId() == null
                || periode == null
                || periode.isBlank()) {
            return null;
        }

        return evaluationConduiteRepository
                .findByInscriptionIdAndPeriode(
                        inscription.getId(),
                        periode.trim()
                )
                .orElse(null);
    }

    /**
     * Vérifie si une note classique correspond à une ancienne matière
     * Conduite. Cette matière ne doit plus être comptée, car la conduite
     * est maintenant gérée par EvaluationConduite.
     */
    private boolean estConduite(Note note) {
        if (note == null || note.getMatiere() == null) {
            return false;
        }

        String nom = note.getMatiere().getNom();

        return nom != null
                && nom.trim().equalsIgnoreCase("Conduite");
    }

    private double safe(Double valeur) {
        return valeur == null ? 0.0 : valeur;
    }

    private double safeConduite(EvaluationConduite conduite) {
        if (conduite == null || conduite.getNote() == null) {
            return 0.0;
        }

        return conduite.getNote().doubleValue();
    }

    // =========================================================
    // NOTES DES MATIÈRES PROGRAMMÉES
    // =========================================================

    public List<Note> construireNotesPourBulletin(
            Inscription inscription,
            Long classeId,
            Long anneeScolaireId,
            String periode
    ) {
        if (inscription == null || inscription.getEleve() == null) {
            return new ArrayList<>();
        }

        Eleve eleve = inscription.getEleve();

        List<AffectationEnseignant> affectations =
                affectationEnseignantRepository
                        .findByClasseIdAndCoefficientMatiere_AnneeScolaireId(
                                classeId,
                                anneeScolaireId
                        );

        Map<Long, CoefficientMatiere> programmesParId =
                new LinkedHashMap<>();

        if (affectations != null) {
            for (AffectationEnseignant affectation : affectations) {
                if (affectation == null) {
                    continue;
                }

                CoefficientMatiere programme =
                        affectation.getCoefficientMatiere();

                if (programme == null || programme.getId() == null) {
                    continue;
                }

                // Ne pas traiter la conduite comme une matière classique.
                if (programme.getMatiere() != null
                        && programme.getMatiere().getNom() != null
                        && programme.getMatiere().getNom()
                        .trim().equalsIgnoreCase("Conduite")) {
                    continue;
                }

                programmesParId.putIfAbsent(
                        programme.getId(),
                        programme
                );
            }
        }

        List<Note> resultat = new ArrayList<>();

        for (CoefficientMatiere programme : programmesParId.values()) {

            if (!estCompatibleAvecSousGroupe(eleve, programme)) {
                continue;
            }

            Note note = trouverNoteExistante(
                    inscription,
                    programme,
                    periode
            );

            if (note == null) {
                // Note temporaire pour afficher la matière même
                // lorsqu'aucune note n'a encore été saisie.
                note = new Note();
                note.setInscription(inscription);
                note.setEleve(eleve);
                note.setClasse(inscription.getClasse());
                note.setAnneeScolaire(inscription.getAnneeScolaire());
                note.setCoefficientMatiere(programme);
                note.setMatiere(programme.getMatiere());
                note.setCoeff(programme.getCoefficient());
                note.setSousGroupe(programme.getSousGroupe());
                note.setPeriode(periode);
            }

            resultat.add(note);
        }

        return resultat;
    }

    private Note trouverNoteExistante(
            Inscription inscription,
            CoefficientMatiere programme,
            String periode
    ) {
        if (programme.getSousGroupe() != null) {
            return noteRepository
                    .findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeId(
                            inscription.getId(),
                            programme.getId(),
                            periode,
                            programme.getSousGroupe().getId()
                    )
                    .orElse(null);
        }

        return noteRepository
                .findByInscriptionIdAndCoefficientMatiereIdAndPeriodeAndSousGroupeIsNull(
                        inscription.getId(),
                        programme.getId(),
                        periode
                )
                .orElse(null);
    }

    private boolean estCompatibleAvecSousGroupe(
            Eleve eleve,
            CoefficientMatiere programme
    ) {
        if (programme.getSousGroupe() == null) {
            return true;
        }

        if (eleve.getSousGroupes() == null
                || eleve.getSousGroupes().isEmpty()) {
            return false;
        }

        Long sousGroupeId = programme.getSousGroupe().getId();

        return eleve.getSousGroupes().stream()
                .anyMatch(sg ->
                        sg != null
                                && sg.getId() != null
                                && sg.getId().equals(sousGroupeId)
                );
    }

    // =========================================================
    // CALCUL DES MOYENNES
    // =========================================================

    /**
     * Moyenne d'une matière :
     * (note de classe + 2 * note de composition) / 3.
     */
    private double calculerMoyenneNote(Note note) {
        return (safe(note.getNClass())
                + 2.0 * safe(note.getNExem())) / 3.0;
    }

    /**
     * Moyenne générale classique, conservée pour les appels
     * qui ne disposent pas de l'évaluation de conduite.
     */
    private double calculerMoyenneGenerale(List<Note> notes) {
        return calculerMoyenneGenerale(notes, null);
    }

    /**
     * Moyenne générale avec conduite indépendante.
     *
     * Les matières classiques sont pondérées par leur coefficient.
     * La conduite compte avec un coefficient fixe de 1.
     * Une conduite non encore notée n'est pas incluse dans le calcul.
     */
    private double calculerMoyenneGenerale(
            List<Note> notes,
            EvaluationConduite conduite
    ) {
        double totalPoints = 0.0;
        double totalCoeff = 0.0;

        if (notes != null) {
            for (Note note : notes) {
                if (note == null || estConduite(note)) {
                    continue;
                }

                double moyenne = calculerMoyenneNote(note);
                double coefficient = note.getCoeff() == null
                        ? 1.0
                        : note.getCoeff().doubleValue();

                if (coefficient <= 0) {
                    continue;
                }

                totalPoints += moyenne * coefficient;
                totalCoeff += coefficient;
            }
        }

        if (conduite != null && conduite.getNote() != null) {
            totalPoints += conduite.getNote().doubleValue();
            totalCoeff += 1.0;
        }

        return totalCoeff == 0.0
                ? 0.0
                : totalPoints / totalCoeff;
    }

    private List<Double> calculerMoyennesClasse(
            Long classeId,
            Long anneeScolaireId,
            String periode
    ) {
        List<Inscription> inscriptions =
                inscriptionRepository.findElevesValidesPourReleve(
                        classeId,
                        anneeScolaireId
                );

        List<Double> moyennes = new ArrayList<>();

        if (inscriptions == null) {
            return moyennes;
        }

        for (Inscription inscription : inscriptions) {
            if (inscription == null || inscription.getEleve() == null) {
                continue;
            }

            List<Note> notes = construireNotesPourBulletin(
                    inscription,
                    classeId,
                    anneeScolaireId,
                    periode
            );

            EvaluationConduite conduite =
                    trouverConduite(inscription, periode);

            moyennes.add(
                    calculerMoyenneGenerale(notes, conduite)
            );
        }

        return moyennes;
    }

    // =========================================================
    // BULLETIN POUR ENVOI
    // =========================================================

    public byte[] genererBulletinPourEnvoi(
            Long inscriptionId,
            Long classeId,
            Long anneeScolaireId,
            String periode
    ) {
        Inscription inscription = inscriptionRepository
                .findById(inscriptionId)
                .orElseThrow(() ->
                        new RuntimeException("Inscription introuvable")
                );

        if (inscription.getEleve() == null) {
            throw new RuntimeException(
                    "Aucun élève associé à cette inscription"
            );
        }

        List<Note> notes = construireNotesPourBulletin(
                inscription,
                classeId,
                anneeScolaireId,
                periode
        );

        List<Double> moyennesDeLaClasse = calculerMoyennesClasse(
                classeId,
                anneeScolaireId,
                periode
        );

        if (moyennesDeLaClasse.isEmpty()) {
            EvaluationConduite conduite =
                    trouverConduite(inscription, periode);

            moyennesDeLaClasse.add(
                    calculerMoyenneGenerale(notes, conduite)
            );
        }

        return generateBulletin(
                notes,
                inscription.getEleve(),
                periode,
                inscription,
                moyennesDeLaClasse
        );
    }

    // =========================================================
    // GÉNÉRATION PDF — BULLETIN UNIQUE
    // =========================================================

    public byte[] generateBulletin(
            List<Note> notes,
            Eleve eleve,
            String periode
    ) {
        return generateBulletin(
                notes,
                eleve,
                periode,
                null,
                null
        );
    }

    public byte[] generateBulletin(
            List<Note> notes,
            Eleve eleve,
            String periode,
            Inscription inscription,
            List<Double> moyennesDeLaClasse
    ) {
        try {
            if (eleve == null) {
                throw new RuntimeException("Élève introuvable");
            }

            if (notes == null) {
                notes = new ArrayList<>();
            }

            if (notes.isEmpty()) {
                throw new RuntimeException(
                        "Aucune matière programmée pour cette classe"
                );
            }

            EvaluationConduite conduite =
                    trouverConduite(inscription, periode);

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document doc = new Document(
                    PageSize.A4,
                    30,
                    30,
                    20,
                    20
            );

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            genererBulletinDansDocument(
                    doc,
                    notes,
                    eleve,
                    periode,
                    conduite,
                    inscription,
                    moyennesDeLaClasse,
                    resolveEcole(inscription)
            );

            doc.close();
            writer.close();

            return out.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();

            throw new RuntimeException(
                    "Erreur PDF : " + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // DTO BULLETIN
    // =========================================================

    public BulletinDTO getBulletin(
            Long inscriptionId,
            Long classeId,
            Long anneeId
    ) {
        Inscription inscription = inscriptionRepository
                .findById(inscriptionId)
                .orElseThrow(() ->
                        new RuntimeException("Inscription introuvable")
                );

        if (inscription.getEleve() == null) {
            throw new RuntimeException(
                    "Aucun élève associé à cette inscription"
            );
        }

        List<Note> notes = noteRepository
                .findByEleveIdAndClasseIdAndAnneeScolaireId(
                        inscription.getEleve().getId(),
                        classeId,
                        anneeId
                );

        double total = 0.0;
        double totalCoeff = 0.0;

        for (Note note : notes) {
            if (note == null || estConduite(note)) {
                continue;
            }

            double moyenne = calculerMoyenneNote(note);
            double coefficient = note.getCoeff() == null
                    ? 1.0
                    : note.getCoeff().doubleValue();

            if (coefficient <= 0) {
                continue;
            }

            total += moyenne * coefficient;
            totalCoeff += coefficient;
        }

        double moyenneAnnuelle = totalCoeff == 0
                ? 0.0
                : total / totalCoeff;

        BulletinDTO dto = new BulletinDTO();
        dto.setEleveId(inscription.getEleve().getId());
        dto.setMoyenneAnnuelle(moyenneAnnuelle);
        dto.setMention(getMention(moyenneAnnuelle));

        return dto;
    }

    // =========================================================
    // MENTIONS ET APPRÉCIATIONS
    // =========================================================

    private String getMention(double moyenne) {
        if (moyenne < 10) return "Insuffisant";
        if (moyenne < 12) return "Passable";
        if (moyenne < 14) return "Assez Bien";
        if (moyenne < 16) return "Bien";
        return "Très Bien";
    }

    private String getAppreciationMatiere(double moyenne) {
        if (moyenne < 10) return "Insuffisant";
        if (moyenne < 12) return "Passable";
        if (moyenne < 14) return "A Bien";
        if (moyenne < 16) return "Bien";
        if (moyenne < 18) return "T.Bien";
        return "Excellent";
    }

    public int calculRang(
            List<Double> moyennes,
            double moyenneEleve
    ) {
        if (moyennes == null) {
            return 1;
        }

        return (int) moyennes.stream()
                .filter(m -> m != null && m > moyenneEleve)
                .count() + 1;
    }

    // =========================================================
    // BULLETINS DE TOUTE UNE CLASSE
    // =========================================================

    public byte[] generateBulletinsClasse(
            Long classeId,
            Long anneeScolaireId,
            String periode
    ) {
        try {
            List<Inscription> inscriptions =
                    inscriptionRepository.findElevesValidesPourReleve(
                            classeId,
                            anneeScolaireId
                    );

            if (inscriptions == null || inscriptions.isEmpty()) {
                throw new RuntimeException(
                        "Aucun élève valide dans cette classe pour cette année scolaire"
                );
            }

            Map<Long, List<Note>> notesParInscription =
                    new LinkedHashMap<>();

            Map<Long, Double> moyenneParInscription =
                    new LinkedHashMap<>();

            for (Inscription inscription : inscriptions) {
                if (inscription == null
                        || inscription.getEleve() == null) {
                    continue;
                }

                List<Note> notes = construireNotesPourBulletin(
                        inscription,
                        classeId,
                        anneeScolaireId,
                        periode
                );

                EvaluationConduite conduite =
                        trouverConduite(inscription, periode);

                notesParInscription.put(
                        inscription.getId(),
                        notes
                );

                moyenneParInscription.put(
                        inscription.getId(),
                        calculerMoyenneGenerale(notes, conduite)
                );
            }

            List<Double> moyennesDeLaClasse =
                    new ArrayList<>(moyenneParInscription.values());

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document doc = new Document(
                    PageSize.A4,
                    30,
                    30,
                    20,
                    20
            );

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            boolean bulletinGenere = false;

            for (Inscription inscription : inscriptions) {
                if (inscription == null
                        || inscription.getEleve() == null) {
                    continue;
                }

                List<Note> notes =
                        notesParInscription.get(inscription.getId());

                if (notes == null || notes.isEmpty()) {
                    continue;
                }

                if (bulletinGenere) {
                    doc.newPage();
                }

                EvaluationConduite conduite =
                        trouverConduite(inscription, periode);

                genererBulletinDansDocument(
                        doc,
                        notes,
                        inscription.getEleve(),
                        periode,
                        conduite,
                        inscription,
                        moyennesDeLaClasse,
                        resolveEcole(inscription)
                );

                bulletinGenere = true;
            }

            if (!bulletinGenere) {
                doc.close();
                writer.close();

                throw new RuntimeException(
                        "Aucun bulletin ne peut être généré pour cette classe"
                );
            }

            doc.close();
            writer.close();

            return out.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();

            throw new RuntimeException(
                    "Erreur génération des bulletins de la classe : "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // BULLETINS COMPACTS
    // =========================================================

    public byte[] generateBulletinsClasseCompact(
            Long classeId,
            Long anneeScolaireId,
            String periode,
            int bulletinsParPage
    ) {
        int parPage = bulletinsParPage == 4 ? 4 : 2;
        int colonnes = parPage == 4 ? 2 : 1;
        int lignes = (int) Math.ceil(parPage / (double) colonnes);
        float echelle = parPage == 4 ? 0.42f : 0.62f;

        try {
            List<Inscription> inscriptions =
                    inscriptionRepository.findElevesValidesPourReleve(
                            classeId,
                            anneeScolaireId
                    );

            if (inscriptions == null || inscriptions.isEmpty()) {
                throw new RuntimeException(
                        "Aucun élève valide dans cette classe pour cette année scolaire"
                );
            }

            List<Inscription> inscriptionsValides =
                    new ArrayList<>();

            Map<Long, List<Note>> notesParInscription =
                    new LinkedHashMap<>();

            List<Double> moyennesDeLaClasse =
                    new ArrayList<>();

            for (Inscription inscription : inscriptions) {
                if (inscription == null
                        || inscription.getEleve() == null) {
                    continue;
                }

                List<Note> notes = construireNotesPourBulletin(
                        inscription,
                        classeId,
                        anneeScolaireId,
                        periode
                );

                if (notes == null || notes.isEmpty()) {
                    continue;
                }

                EvaluationConduite conduite =
                        trouverConduite(inscription, periode);

                inscriptionsValides.add(inscription);
                notesParInscription.put(
                        inscription.getId(),
                        notes
                );

                moyennesDeLaClasse.add(
                        calculerMoyenneGenerale(notes, conduite)
                );
            }

            if (inscriptionsValides.isEmpty()) {
                throw new RuntimeException(
                        "Aucun bulletin ne peut être généré pour cette classe"
                );
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document doc = new Document(
                    PageSize.A4,
                    15,
                    15,
                    15,
                    15
            );

            PdfWriter writer = PdfWriter.getInstance(doc, out);
            doc.open();

            for (int debut = 0;
                 debut < inscriptionsValides.size();
                 debut += parPage) {

                List<Inscription> groupe =
                        inscriptionsValides.subList(
                                debut,
                                Math.min(
                                        debut + parPage,
                                        inscriptionsValides.size()
                                )
                        );

                PdfPTable grille = new PdfPTable(colonnes);
                grille.setWidthPercentage(100);

                for (Inscription inscription : groupe) {
                    List<Note> notes =
                            notesParInscription.get(inscription.getId());

                    EvaluationConduite conduite =
                            trouverConduite(inscription, periode);

                    PdfPTable miniBulletin =
                            construireContenuBulletin(
                                    notes,
                                    inscription.getEleve(),
                                    periode,
                                    conduite,
                                    inscription,
                                    moyennesDeLaClasse,
                                    resolveEcole(inscription),
                                    echelle
                            );

                    PdfPCell caseGrille = new PdfPCell();
                    caseGrille.setBorder(Rectangle.BOX);
                    caseGrille.setBorderColor(BaseColor.GRAY);
                    caseGrille.setPadding(6);
                    caseGrille.addElement(miniBulletin);

                    grille.addCell(caseGrille);
                }

                int manquants = lignes * colonnes - groupe.size();

                for (int i = 0; i < manquants; i++) {
                    PdfPCell vide = new PdfPCell();
                    vide.setBorder(Rectangle.NO_BORDER);
                    grille.addCell(vide);
                }

                doc.add(grille);

                boolean dernierGroupe =
                        debut + parPage >= inscriptionsValides.size();

                if (!dernierGroupe) {
                    doc.newPage();
                }
            }

            doc.close();
            writer.close();

            return out.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();

            throw new RuntimeException(
                    "Erreur génération des bulletins compacts : "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =========================================================
    // CONTENU DU BULLETIN
    // =========================================================

    private PdfPTable construireContenuBulletin(
            List<Note> notes,
            Eleve eleve,
            String periode,
            EvaluationConduite conduite,
            Inscription inscription,
            List<Double> moyennesDeLaClasse,
            Ecole ecole,
            float echelle
    ) throws DocumentException {

        Font ecoleFont = new Font(
                Font.FontFamily.TIMES_ROMAN,
                20 * echelle,
                Font.BOLD
        );

        Font adresseFont = new Font(
                Font.FontFamily.TIMES_ROMAN,
                10 * echelle,
                Font.ITALIC
        );

        Font titreFont = new Font(
                Font.FontFamily.TIMES_ROMAN,
                16 * echelle,
                Font.BOLD
        );

        Font labelFont = new Font(
                Font.FontFamily.HELVETICA,
                11 * echelle,
                Font.BOLD
        );

        Font valeurFont = new Font(
                Font.FontFamily.HELVETICA,
                11 * echelle
        );

        Font tableHeaderFont = new Font(
                Font.FontFamily.HELVETICA,
                9 * echelle,
                Font.BOLD
        );

        Font tableCellFont = new Font(
                Font.FontFamily.HELVETICA,
                10 * echelle
        );

        Font grandTitreFont = new Font(
                Font.FontFamily.TIMES_ROMAN,
                16 * echelle,
                Font.BOLD
        );

        PdfPCell contenuCell = new PdfPCell();
        contenuCell.setBorder(Rectangle.NO_BORDER);
        contenuCell.setPadding(Math.max(1f, 4 * echelle));

        // -----------------------------------------------------
        // EN-TÊTE ÉTABLISSEMENT
        // -----------------------------------------------------

        String ecoleNom =
                ecole != null && ecole.getNom() != null
                        ? ecole.getNom().toUpperCase()
                        : ECOLE_NOM_DEFAUT;

        String ligneAdresse = ECOLE_ADRESSE_DEFAUT;
        String ligneContact = "";

        if (ecole != null) {
            List<String> adresses = new ArrayList<>();

            if (ecole.getAdresse() != null
                    && !ecole.getAdresse().isBlank()) {
                adresses.add(ecole.getAdresse());
            }

            if (ecole.getVille() != null
                    && !ecole.getVille().isBlank()) {
                adresses.add(ecole.getVille());
            }

            if (ecole.getPays() != null
                    && !ecole.getPays().isBlank()) {
                adresses.add(ecole.getPays());
            }

            ligneAdresse = String.join(", ", adresses);

            List<String> contacts = new ArrayList<>();

            if (ecole.getTelephone() != null
                    && !ecole.getTelephone().isBlank()) {
                contacts.add("Tél : " + ecole.getTelephone());
            }

            if (ecole.getEmail() != null
                    && !ecole.getEmail().isBlank()) {
                contacts.add("Email : " + ecole.getEmail());
            }

            ligneContact = String.join(" ; ", contacts);
        }

        Image logoImage = null;

        if (ecole != null
                && ecole.getLogo() != null
                && !ecole.getLogo().isBlank()) {
            try {
                logoImage = Image.getInstance(ecole.getLogo());
                logoImage.scaleToFit(60, 60);
            } catch (Exception e) {
                System.out.println(
                        "Logo établissement illisible : " + e.getMessage()
                );
            }
        }

        if (logoImage != null) {
            PdfPTable entete = new PdfPTable(new float[]{1f, 5f});
            entete.setWidthPercentage(100);

            PdfPCell logoCell = new PdfPCell(logoImage, false);
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            entete.addCell(logoCell);

            PdfPCell nomCell = new PdfPCell(
                    new Phrase(ecoleNom, ecoleFont)
            );
            nomCell.setBorder(Rectangle.NO_BORDER);
            nomCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            nomCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            entete.addCell(nomCell);

            contenuCell.addElement(entete);
        } else {
            PdfPTable ecoleBox = new PdfPTable(1);
            ecoleBox.setWidthPercentage(100);

            PdfPCell ecoleCell = new PdfPCell(
                    new Phrase(ecoleNom, ecoleFont)
            );
            ecoleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            ecoleCell.setPadding(8);
            ecoleBox.addCell(ecoleCell);

            contenuCell.addElement(ecoleBox);
        }

        if (!ligneAdresse.isBlank()) {
            Paragraph adresse = new Paragraph(
                    ligneAdresse,
                    adresseFont
            );
            adresse.setAlignment(Element.ALIGN_CENTER);
            contenuCell.addElement(adresse);
        }

        if (!ligneContact.isBlank()) {
            Paragraph contact = new Paragraph(
                    ligneContact,
                    adresseFont
            );
            contact.setAlignment(Element.ALIGN_CENTER);
            contenuCell.addElement(contact);
        }

        contenuCell.addElement(new Paragraph(" "));

        PdfPTable titreBox = new PdfPTable(1);
        titreBox.setWidthPercentage(60);
        titreBox.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell titreCell = new PdfPCell(
                new Phrase("BULLETIN DE NOTES", titreFont)
        );
        titreCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titreCell.setPadding(10);
        titreBox.addCell(titreCell);

        contenuCell.addElement(titreBox);
        contenuCell.addElement(new Paragraph(" "));

        // -----------------------------------------------------
        // INFORMATIONS ÉLÈVE
        // -----------------------------------------------------

        String anneeScolaire =
                inscription != null
                        && inscription.getAnneeScolaire() != null
                        ? String.valueOf(
                        inscription.getAnneeScolaire().getNom()
                )
                        : "-";

        String classeNom =
                inscription != null
                        && inscription.getClasse() != null
                        ? String.valueOf(
                        inscription.getClasse().getNomComplet()
                )
                        : "-";

        String matricule = eleve.getMatricule() != null
                ? String.valueOf(eleve.getMatricule())
                : "-";

        PdfPTable infos = new PdfPTable(2);
        infos.setWidthPercentage(100);
        infos.setWidths(new float[]{1f, 1f});

        infos.addCell(infoCell(
                "Année scolaire : " + anneeScolaire,
                labelFont
        ));

        infos.addCell(infoCellDroite(
                "Nom : ",
                eleve.getNom(),
                labelFont,
                valeurFont
        ));

        infos.addCell(infoCell(periode, labelFont));

        infos.addCell(infoCellDroite(
                "Prénom : ",
                eleve.getPrenom(),
                labelFont,
                valeurFont
        ));

        infos.addCell(infoCell(
                "Classe : " + classeNom,
                labelFont
        ));

        infos.addCell(infoCellDroite(
                "N° matricule : ",
                matricule,
                labelFont,
                valeurFont
        ));

        contenuCell.addElement(infos);
        contenuCell.addElement(new Paragraph(" "));

        // -----------------------------------------------------
        // TABLEAU DES MATIÈRES ET DE LA CONDUITE
        // -----------------------------------------------------

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(
                new float[]{3f, 1.3f, 1.3f, 1.3f, 0.8f, 1.3f, 2.2f}
        );

        addHeader(table, "Matières", tableHeaderFont);
        addHeader(table, "Moyen.\nClasse", tableHeaderFont);
        addHeader(table, "Notes\nCompo.", tableHeaderFont);
        addHeader(table, "Moyen.\nNotes", tableHeaderFont);
        addHeader(table, "Coef", tableHeaderFont);
        addHeader(table, "Moyen.\nCoef", tableHeaderFont);
        addHeader(table, "Appréciations", tableHeaderFont);

        double totalGeneral = 0.0;
        double totalCoeff = 0.0;

        if (notes != null) {
            for (Note note : notes) {
                if (note == null || estConduite(note)) {
                    continue;
                }

                String matiereNom =
                        note.getMatiere() != null
                                && note.getMatiere().getNom() != null
                                ? note.getMatiere().getNom()
                                : "N/A";

                double coefficient = note.getCoeff() == null
                        ? 1.0
                        : note.getCoeff().doubleValue();

                double noteClasse = safe(note.getNClass());
                double noteComposition = safe(note.getNExem());
                double moyenne = calculerMoyenneNote(note);
                double points = moyenne * coefficient;

                if (coefficient > 0) {
                    totalGeneral += points;
                    totalCoeff += coefficient;
                }

                table.addCell(bodyCell(
                        matiereNom,
                        tableCellFont,
                        Element.ALIGN_LEFT
                ));

                table.addCell(bodyCell(
                        formatNote(noteClasse),
                        tableCellFont,
                        Element.ALIGN_CENTER
                ));

                table.addCell(bodyCell(
                        formatNote(noteComposition),
                        tableCellFont,
                        Element.ALIGN_CENTER
                ));

                table.addCell(bodyCell(
                        formatNote(moyenne),
                        tableCellFont,
                        Element.ALIGN_CENTER
                ));

                table.addCell(bodyCell(
                        formatNote(coefficient),
                        tableCellFont,
                        Element.ALIGN_CENTER
                ));

                table.addCell(bodyCell(
                        formatNote(points),
                        tableCellFont,
                        Element.ALIGN_CENTER
                ));

                table.addCell(bodyCell(
                        getAppreciationMatiere(moyenne),
                        tableCellFont,
                        Element.ALIGN_CENTER
                ));
            }
        }

        // Ligne de conduite indépendante : coefficient fixe égal à 1.
        if (conduite != null && conduite.getNote() != null) {
            double noteConduite = safeConduite(conduite);

            totalGeneral += noteConduite;
            totalCoeff += 1.0;

            String appreciation = conduite.getAppreciation();

            if (appreciation == null || appreciation.isBlank()) {
                appreciation = getAppreciationMatiere(noteConduite);
            }

            table.addCell(bodyCell(
                    "Conduite",
                    tableCellFont,
                    Element.ALIGN_LEFT
            ));

            table.addCell(bodyCell(
                    "-",
                    tableCellFont,
                    Element.ALIGN_CENTER
            ));

            table.addCell(bodyCell(
                    "-",
                    tableCellFont,
                    Element.ALIGN_CENTER
            ));

            table.addCell(bodyCell(
                    formatNote(noteConduite),
                    tableCellFont,
                    Element.ALIGN_CENTER
            ));

            table.addCell(bodyCell(
                    "1",
                    tableCellFont,
                    Element.ALIGN_CENTER
            ));

            table.addCell(bodyCell(
                    formatNote(noteConduite),
                    tableCellFont,
                    Element.ALIGN_CENTER
            ));

            table.addCell(bodyCell(
                    appreciation,
                    tableCellFont,
                    Element.ALIGN_CENTER
            ));
        }

        contenuCell.addElement(table);

        // -----------------------------------------------------
        // TOTAUX
        // -----------------------------------------------------

        PdfPTable totaux = new PdfPTable(2);
        totaux.setWidthPercentage(60);
        totaux.setWidths(new float[]{1.3f, 1f});

        totaux.addCell(totalCell(
                "Total général :",
                labelFont,
                Element.ALIGN_LEFT
        ));

        totaux.addCell(totalCell(
                formatNote(totalGeneral),
                valeurFont,
                Element.ALIGN_CENTER
        ));

        totaux.addCell(totalCell(
                "Total coefficients :",
                labelFont,
                Element.ALIGN_LEFT
        ));

        totaux.addCell(totalCell(
                formatNote(totalCoeff),
                valeurFont,
                Element.ALIGN_CENTER
        ));

        contenuCell.addElement(totaux);
        contenuCell.addElement(new Paragraph(" "));

        // -----------------------------------------------------
        // MOYENNE, RANG ET MOYENNE DU PREMIER
        // -----------------------------------------------------

        double moyenneGenerale = totalCoeff == 0
                ? 0.0
                : totalGeneral / totalCoeff;

        String rangTexte = "-";
        String moyenne1erTexte = "-";

        if (moyennesDeLaClasse != null
                && !moyennesDeLaClasse.isEmpty()) {

            int rang = calculRang(
                    moyennesDeLaClasse,
                    moyenneGenerale
            );

            rangTexte = rang + " / " + moyennesDeLaClasse.size();

            double moyennePremier = moyennesDeLaClasse.stream()
                    .filter(m -> m != null)
                    .max(Comparator.naturalOrder())
                    .orElse(moyenneGenerale);

            moyenne1erTexte = formatNote(moyennePremier);
        }

        PdfPTable moyenneRang = new PdfPTable(2);
        moyenneRang.setWidthPercentage(100);

        moyenneRang.addCell(boxCell(
                "Moyenne : " + formatNote(moyenneGenerale),
                labelFont
        ));

        moyenneRang.addCell(boxCell(
                "Rang : " + rangTexte,
                labelFont
        ));

        contenuCell.addElement(moyenneRang);
        contenuCell.addElement(new Paragraph(" "));

        PdfPTable moyenne1erTable = new PdfPTable(1);
        moyenne1erTable.setWidthPercentage(50);
        moyenne1erTable.setHorizontalAlignment(Element.ALIGN_LEFT);

        moyenne1erTable.addCell(boxCell(
                "Moyenne du 1er : " + moyenne1erTexte,
                labelFont
        ));

        contenuCell.addElement(moyenne1erTable);
        contenuCell.addElement(new Paragraph(" "));

        // -----------------------------------------------------
        // APPRÉCIATION GÉNÉRALE
        // -----------------------------------------------------

        PdfPTable appreciationBox = new PdfPTable(1);
        appreciationBox.setWidthPercentage(60);
        appreciationBox.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell appreciationTitre = new PdfPCell(
                new Phrase(
                        "APPRÉCIATIONS GÉNÉRALES",
                        tableHeaderFont
                )
        );

        appreciationTitre.setHorizontalAlignment(Element.ALIGN_CENTER);
        appreciationTitre.setBackgroundColor(BaseColor.LIGHT_GRAY);
        appreciationBox.addCell(appreciationTitre);

        PdfPCell appreciationValeur = new PdfPCell(
                new Phrase(
                        getMention(moyenneGenerale).toUpperCase(),
                        grandTitreFont
                )
        );

        appreciationValeur.setHorizontalAlignment(Element.ALIGN_CENTER);
        appreciationValeur.setPadding(10);
        appreciationBox.addCell(appreciationValeur);

        contenuCell.addElement(appreciationBox);

        // -----------------------------------------------------
        // SIGNATURES
        // -----------------------------------------------------

        contenuCell.addElement(new Paragraph(" "));
        contenuCell.addElement(new Paragraph(" "));

        PdfPTable signatures = new PdfPTable(2);
        signatures.setWidthPercentage(100);
        signatures.addCell(noBorder("PROVISEUR", labelFont));
        signatures.addCell(noBorder("PARENT", labelFont));

        contenuCell.addElement(signatures);

        PdfPTable conteneur = new PdfPTable(1);
        conteneur.setWidthPercentage(100);
        conteneur.addCell(contenuCell);

        return conteneur;
    }

    // =========================================================
    // AJOUT AU DOCUMENT PDF
    // =========================================================

    private void genererBulletinDansDocument(
            Document doc,
            List<Note> notes,
            Eleve eleve,
            String periode,
            EvaluationConduite conduite,
            Inscription inscription,
            List<Double> moyennesDeLaClasse,
            Ecole ecole
    ) throws DocumentException {

        doc.add(construireContenuBulletin(
                notes,
                eleve,
                periode,
                conduite,
                inscription,
                moyennesDeLaClasse,
                ecole,
                1.0f
        ));
    }

    // =========================================================
    // HELPERS PDF
    // =========================================================

    private String formatNote(double valeur) {
        return String.format(
                java.util.Locale.FRANCE,
                "%.2f",
                valeur
        ).replace(",00", "").replace('.', ',');
    }

    private PdfPCell addHeader(
            PdfPTable table,
            String texte,
            Font font
    ) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        table.addCell(cell);
        return cell;
    }

    private PdfPCell bodyCell(
            String texte,
            Font font,
            int alignment
    ) {
        PdfPCell cell = new PdfPCell(
                new Phrase(texte == null ? "-" : texte, font)
        );
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell totalCell(
            String texte,
            Font font,
            int alignment
    ) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private PdfPCell boxCell(String texte, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell infoCell(String texte, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private PdfPCell infoCellDroite(
            String label,
            String valeur,
            Font labelFont,
            Font valeurFont
    ) {
        Phrase phrase = new Phrase();
        phrase.add(new Chunk(label, labelFont));
        phrase.add(new Chunk(
                valeur == null ? "-" : valeur,
                valeurFont
        ));

        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private PdfPCell noBorder(String texte, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texte, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPaddingTop(30);
        return cell;
    }
}
