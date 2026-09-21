package com.saas.school.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.saas.school.dto.BulletinDTO;
import com.saas.school.entity.AffectationEnseignant;
import com.saas.school.entity.CoefficientMatiere;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Eleve;
import com.saas.school.entity.Inscription;
import com.saas.school.entity.Note;
import com.saas.school.repository.AffectationEnseignantRepository;
import com.saas.school.repository.EcoleRepository;
import com.saas.school.repository.InscriptionRepository;
import com.saas.school.repository.NoteRepository;
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

    // ================= EN-TÊTE ÉTABLISSEMENT (repli si aucune Ecole trouvée) =================
    private static final String ECOLE_NOM_DEFAUT = "ETABLISSEMENT";
    private static final String ECOLE_ADRESSE_DEFAUT = "";

    private final NoteRepository noteRepository;
    private final InscriptionRepository inscriptionRepository;
    // FIX: injected so we can build the bulletin from what's actually
    // affected to the class (AffectationEnseignant) instead of only from
    // rows that already exist in Note.
    private final AffectationEnseignantRepository affectationEnseignantRepository;
    // Utilisé pour afficher le nom / l'adresse réels de l'établissement dans
    // l'en-tête du bulletin (au lieu de valeurs codées en dur).
    private final EcoleRepository ecoleRepository;

    /**
     * Résout l'Ecole à afficher dans l'en-tête du bulletin.
     * HYPOTHÈSE À VÉRIFIER : le lien passe par Classe.getEcole(). Si votre
     * modèle relie plutôt l'école à AnneeScolaire, ou passe par un contexte
     * multi-tenant, adaptez cette méthode en conséquence.
     */
    private Ecole resolveEcole(Inscription inscription) {
        if (inscription == null) {
            return null;
        }
        try {
            if (inscription.getClasse() != null && inscription.getClasse().getEcole() != null) {
                return inscription.getClasse().getEcole();
            }
        } catch (Exception e) {
            // getClasse()/getEcole() indisponible : on retombe sur les valeurs par défaut.
            System.out.println("⚠️ Impossible de résoudre l'Ecole depuis l'inscription : " + e.getMessage());
        }
        return null;
    }

    /**
     * FIX (nouveau) — construit la liste des notes à afficher sur le bulletin
     * en partant des MATIÈRES AFFECTÉES à la classe (AffectationEnseignant),
     * pas des notes déjà saisies. Toute matière affectée à la classe de
     * l'élève (et compatible avec ses sous-groupes) apparaît sur le
     * bulletin, avec une Note "vide" (non persistée) si rien n'a encore été
     * saisi.
     */
    public List<Note> construireNotesPourBulletin(
            Inscription inscription,
            Long classeId,
            Long anneeScolaireId,
            String periode
    ) {
        Eleve eleve = inscription.getEleve();

        List<AffectationEnseignant> affectations = affectationEnseignantRepository
                .findByClasseIdAndCoefficientMatiere_AnneeScolaireId(classeId, anneeScolaireId);

        // Une matière peut apparaître plusieurs fois (plusieurs enseignants,
        // plusieurs sous-groupes) : on déduplique par CoefficientMatiere.id
        // en gardant l'ordre d'apparition.
        Map<Long, CoefficientMatiere> programmesParId = new LinkedHashMap<>();

        for (AffectationEnseignant affectation : affectations) {
            CoefficientMatiere programme = affectation.getCoefficientMatiere();

            if (programme != null) {
                programmesParId.putIfAbsent(programme.getId(), programme);
            }
        }

        List<Note> resultat = new ArrayList<>();

        for (CoefficientMatiere programme : programmesParId.values()) {

            // Ignore les matières d'un sous-groupe auquel l'élève n'appartient pas
            if (!estCompatibleAvecSousGroupe(eleve, programme)) {
                continue;
            }

            Note note = trouverNoteExistante(inscription, programme, periode);

            if (note == null) {
                // Aucune note saisie pour cette matière : on l'affiche quand
                // même sur le bulletin, avec des valeurs vides (traitées
                // comme 0 par safe() dans generateBulletin).
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

    private Note trouverNoteExistante(Inscription inscription, CoefficientMatiere programme, String periode) {
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

    private boolean estCompatibleAvecSousGroupe(Eleve eleve, CoefficientMatiere programme) {
        if (programme.getSousGroupe() == null) {
            return true;
        }

        if (eleve.getSousGroupes() == null || eleve.getSousGroupes().isEmpty()) {
            return false;
        }

        Long sousGroupeId = programme.getSousGroupe().getId();
        return eleve.getSousGroupes().stream()
                .anyMatch(sg -> sg != null && sg.getId() != null && sg.getId().equals(sousGroupeId));
    }

    // =====================================================================
    // GÉNÉRATION PDF — bulletin unique
    // =====================================================================

    public byte[] generateBulletin(
            List<Note> notes,
            Eleve eleve,
            String periode
    ) {
        return generateBulletin(notes, eleve, periode, null, null);
    }

    /**
     * Variante complète : permet de fournir l'inscription (pour classe /
     * année scolaire dans l'en-tête) et la liste des moyennes de toute la
     * classe (pour calculer Rang et "Moyenne du 1er" comme sur le modèle
     * papier). Si ces informations ne sont pas disponibles, passer null :
     * les champs correspondants seront simplement omis / affichés à "-".
     */
    public byte[] generateBulletin(
            List<Note> notes,
            Eleve eleve,
            String periode,
            Inscription inscription,
            List<Double> moyennesDeLaClasse
    ) {

        System.out.println("🚀 START PDF GENERATION");

        try {

            if (notes == null || notes.isEmpty()) {
                throw new RuntimeException(
                        "❌ Aucune matière programmée pour cette classe"
                );
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 30, 30, 20, 20);
            PdfWriter writer = PdfWriter.getInstance(doc, out);

            doc.open();

            genererBulletinDansDocument(
                    doc,
                    notes,
                    eleve,
                    periode,
                    inscription,
                    moyennesDeLaClasse,
                    resolveEcole(inscription)
            );

            doc.close();
            writer.close();

            byte[] pdfBytes = out.toByteArray();

            System.out.println("📦 PDF SIZE = " + pdfBytes.length);

            return pdfBytes;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Erreur PDF: " + e.getMessage(),
                    e
            );
        }
    }

    private double safe(Double v) {
        return v == null ? 0.0 : v;
    }

    public BulletinDTO getBulletin(Long inscriptionId, Long classeId, Long anneeId) {
        Inscription inscription = inscriptionRepository.findById(inscriptionId)
                .orElseThrow(() -> new RuntimeException("Inscription introuvable"));

        // NOTE: cette méthode calcule une moyenne annuelle à partir des notes
        // déjà saisies sur TOUTES les périodes (pas de filtre période ici),
        // donc elle ne peut pas réutiliser construireNotesPourBulletin (qui a
        // besoin d'une période précise). Elle garde volontairement son
        // comportement d'origine — si elle doit un jour aussi tenir compte
        // des matières sans note saisie, il faudra l'adapter séparément en
        // itérant sur les périodes de l'année.
        List<Note> notes = noteRepository
                .findByEleveIdAndClasseIdAndAnneeScolaireId(inscription.getEleve().getId(), classeId, anneeId);

        double total = 0;
        double coeffTotal = 0;

        for (Note n : notes) {

            double moyenne = calculerMoyenneNote(n);
            double coeff = n.getCoeff() == null ? 1 : n.getCoeff();

            total += moyenne * coeff;
            coeffTotal += coeff;
        }

        double moyenneAnnuelle = coeffTotal == 0 ? 0 : total / coeffTotal;

        BulletinDTO dto = new BulletinDTO();
        dto.setEleveId(inscription.getEleve().getId());
        dto.setMoyenneAnnuelle(moyenneAnnuelle);
        dto.setMention(getMention(moyenneAnnuelle));

        return dto;
    }

    private String getMention(double m) {
        if (m < 10) return "Insuffisant";
        if (m < 12) return "Passable";
        if (m < 14) return "Assez Bien";
        if (m < 16) return "Bien";
        return "Très Bien";
    }

    /**
     * Appréciation par matière, sur le même barème que le modèle papier
     * (Passable / A Bien / T.Bien / Excellent). Ajustez les seuils si votre
     * établissement utilise un autre barème.
     */
    private String getAppreciationMatiere(double moyenne) {
        if (moyenne < 10) return "Insuffisant";
        if (moyenne < 12) return "Passable";
        if (moyenne < 14) return "A Bien";
        if (moyenne < 16) return "Bien";
        if (moyenne < 18) return "T.Bien";
        return "Excellent";
    }

    public int calculRang(List<Double> moyennes, double eleveMoyenne) {
        return (int) moyennes.stream()
                .filter(m -> m > eleveMoyenne)
                .count() + 1;
    }

    // =====================================================================
    // GÉNÉRATION PDF — bulletins de toute une classe (un par page)
    // =====================================================================

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

            // Pré-calcule la moyenne générale de chaque élève de la classe,
            // pour pouvoir afficher Rang et "Moyenne du 1er" sur chaque
            // bulletin, comme sur le modèle papier.
            Map<Long, List<Note>> notesParInscription = new LinkedHashMap<>();
            Map<Long, Double> moyenneParInscription = new LinkedHashMap<>();

            for (Inscription inscription : inscriptions) {
                if (inscription.getEleve() == null) {
                    continue;
                }
                List<Note> notes = construireNotesPourBulletin(
                        inscription, classeId, anneeScolaireId, periode);

                notesParInscription.put(inscription.getId(), notes);
                moyenneParInscription.put(inscription.getId(), calculerMoyenneGenerale(notes));
            }

            List<Double> moyennesDeLaClasse = new ArrayList<>(moyenneParInscription.values());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 30, 30, 20, 20);
            PdfWriter writer = PdfWriter.getInstance(doc, out);

            doc.open();

            boolean bulletinGenere = false;

            for (Inscription inscription : inscriptions) {

                if (inscription.getEleve() == null) {
                    continue;
                }

                List<Note> notes = notesParInscription.get(inscription.getId());

                if (notes == null || notes.isEmpty()) {
                    continue;
                }

                if (bulletinGenere) {
                    doc.newPage();
                }

                genererBulletinDansDocument(
                        doc,
                        notes,
                        inscription.getEleve(),
                        periode,
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

    // =====================================================================
    // GÉNÉRATION PDF — bulletins de toute une classe, FORMAT COMPACT
    // (plusieurs bulletins réduits sur une même page, comme dans certains
    // établissements qui économisent le papier).
    // =====================================================================

    /**
     * Comme {@link #generateBulletinsClasse}, mais imprime plusieurs
     * bulletins réduits par page A4 au lieu d'un seul.
     *
     * @param bulletinsParPage nombre de bulletins par page : 2 (empilés,
     *                         format le plus courant) ou 4 (grille 2x2,
     *                         plus dense). Toute autre valeur retombe sur 2.
     */
    public byte[] generateBulletinsClasseCompact(
            Long classeId,
            Long anneeScolaireId,
            String periode,
            int bulletinsParPage
    ) {

        int parPage = (bulletinsParPage == 4) ? 4 : 2;
        int colonnes = (parPage == 4) ? 2 : 1;
        int lignes = (int) Math.ceil(parPage / (double) colonnes);
        float echelle = (parPage == 4) ? 0.42f : 0.62f;

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

            // Même logique de pré-calcul que generateBulletinsClasse : on
            // calcule d'abord la moyenne de chaque élève pour pouvoir
            // afficher Rang / Moyenne du 1er sur chaque mini-bulletin.
            List<Inscription> inscriptionsValides = new ArrayList<>();
            Map<Long, List<Note>> notesParInscription = new LinkedHashMap<>();
            List<Double> moyennesDeLaClasse = new ArrayList<>();

            for (Inscription inscription : inscriptions) {
                if (inscription.getEleve() == null) {
                    continue;
                }
                List<Note> notes = construireNotesPourBulletin(
                        inscription, classeId, anneeScolaireId, periode);

                if (notes == null || notes.isEmpty()) {
                    continue;
                }

                inscriptionsValides.add(inscription);
                notesParInscription.put(inscription.getId(), notes);
                moyennesDeLaClasse.add(calculerMoyenneGenerale(notes));
            }

            if (inscriptionsValides.isEmpty()) {
                throw new RuntimeException(
                        "Aucun bulletin ne peut être généré pour cette classe"
                );
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            // Marges réduites : on gagne de la place pour les bulletins compacts.
            Document doc = new Document(PageSize.A4, 15, 15, 15, 15);
            PdfWriter writer = PdfWriter.getInstance(doc, out);

            doc.open();

            for (int debut = 0; debut < inscriptionsValides.size(); debut += parPage) {

                List<Inscription> groupe = inscriptionsValides.subList(
                        debut, Math.min(debut + parPage, inscriptionsValides.size()));

                PdfPTable grille = new PdfPTable(colonnes);
                grille.setWidthPercentage(100);

                for (Inscription inscription : groupe) {

                    List<Note> notes = notesParInscription.get(inscription.getId());
                    Ecole ecole = resolveEcole(inscription);

                    PdfPTable miniBulletin = construireContenuBulletin(
                            notes,
                            inscription.getEleve(),
                            periode,
                            inscription,
                            moyennesDeLaClasse,
                            ecole,
                            echelle
                    );

                    PdfPCell caseGrille = new PdfPCell();
                    caseGrille.setBorder(Rectangle.BOX);
                    caseGrille.setBorderColor(BaseColor.GRAY);
                    caseGrille.setPadding(6);
                    caseGrille.addElement(miniBulletin);
                    grille.addCell(caseGrille);
                }

                // Complète la dernière page si le dernier groupe est incomplet,
                // pour garder une grille régulière (cases vides sans bordure).
                int manquants = (lignes * colonnes) - groupe.size();
                for (int i = 0; i < manquants; i++) {
                    PdfPCell vide = new PdfPCell();
                    vide.setBorder(Rectangle.NO_BORDER);
                    grille.addCell(vide);
                }

                doc.add(grille);

                boolean dernierGroupe = debut + parPage >= inscriptionsValides.size();
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
                    "Erreur génération des bulletins (format compact) de la classe : "
                            + e.getMessage(),
                    e
            );
        }
    }

    // =====================================================================
    // MISE EN PAGE DU BULLETIN (format "Lycée Avenir")
    // =====================================================================

    private boolean estConduite(Note n) {
        String nom = n.getMatiere() != null ? n.getMatiere().getNom() : "";
        return nom != null && nom.trim().equalsIgnoreCase("Conduite");
    }

    /** (moyClasse + moyExamen*2) / 3 — identique au bulletin papier. */
    private double calculerMoyenneNote(Note n) {
        return (safe(n.getNClass()) + safe(n.getNExem()) * 2) / 3;
    }

    private double calculerMoyenneGenerale(List<Note> notes) {
        double totalPoints = 0;
        double totalCoeff = 0;

        for (Note n : notes) {
            if (n == null) continue;

            int coeff = n.getCoeff() != null ? n.getCoeff() : 1;
            double moyenne = estConduite(n) ? safe(n.getNExem()) : calculerMoyenneNote(n);

            totalPoints += moyenne * coeff;
            totalCoeff += coeff;
        }

        return totalCoeff == 0 ? 0 : totalPoints / totalCoeff;
    }

    /**
     * Construit le contenu complet d'un bulletin (en-tête établissement,
     * infos élève, tableau des matières, totaux, appréciation générale,
     * signatures) sous forme d'un PdfPTable autonome.
     * <p>
     * {@code echelle} permet de générer une version réduite (ex: 0.6 pour
     * ~2 bulletins par page, 0.42 pour ~4) : toutes les tailles de police et
     * de marge sont multipliées par cette valeur. Utiliser 1.0 pour le
     * format plein page d'origine.
     */
    private PdfPTable construireContenuBulletin(
            List<Note> notes,
            Eleve eleve,
            String periode,
            Inscription inscription,
            List<Double> moyennesDeLaClasse,
            Ecole ecole,
            float echelle
    ) throws DocumentException {

        Font ecoleFont = new Font(Font.FontFamily.TIMES_ROMAN, 20 * echelle, Font.BOLD);
        Font adresseFont = new Font(Font.FontFamily.TIMES_ROMAN, 10 * echelle, Font.ITALIC);
        Font titreFont = new Font(Font.FontFamily.TIMES_ROMAN, 16 * echelle, Font.BOLD);
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 11 * echelle, Font.BOLD);
        Font valeurFont = new Font(Font.FontFamily.HELVETICA, 11 * echelle);
        Font tableHeaderFont = new Font(Font.FontFamily.HELVETICA, 9 * echelle, Font.BOLD);
        Font tableCellFont = new Font(Font.FontFamily.HELVETICA, 10 * echelle);
        Font grandTitreFont = new Font(Font.FontFamily.TIMES_ROMAN, 16 * echelle, Font.BOLD);

        PdfPCell contenuCell = new PdfPCell();
        contenuCell.setBorder(Rectangle.NO_BORDER);
        contenuCell.setPadding(Math.max(1f, 4 * echelle));

        // ================= EN-TÊTE ÉTABLISSEMENT (données réelles de l'Ecole) =================

        String ecoleNom = (ecole != null && ecole.getNom() != null)
                ? ecole.getNom().toUpperCase()
                : ECOLE_NOM_DEFAUT;

        // Ligne 1 : adresse + ville. Ligne 2 : téléphone + email. On ne
        // construit chaque ligne qu'à partir des champs réellement renseignés.
        String ligneAdresse = "";
        String ligneContact = "";
        if (ecole != null) {
            List<String> partsAdresse = new ArrayList<>();
            if (ecole.getAdresse() != null && !ecole.getAdresse().isBlank()) partsAdresse.add(ecole.getAdresse());
            if (ecole.getVille() != null && !ecole.getVille().isBlank()) partsAdresse.add(ecole.getVille());
            if (ecole.getPays() != null && !ecole.getPays().isBlank()) partsAdresse.add(ecole.getPays());
            ligneAdresse = String.join(", ", partsAdresse);

            List<String> partsContact = new ArrayList<>();
            if (ecole.getTelephone() != null && !ecole.getTelephone().isBlank()) partsContact.add("Tél : " + ecole.getTelephone());
            if (ecole.getEmail() != null && !ecole.getEmail().isBlank()) partsContact.add("Email : " + ecole.getEmail());
            ligneContact = String.join(" ; ", partsContact);
        } else {
            ligneAdresse = ECOLE_ADRESSE_DEFAUT;
        }

        // Logo optionnel : affiché seulement si Ecole.getLogo() pointe vers
        // un fichier/URL lisible par iText. Toute erreur est ignorée pour ne
        // jamais bloquer la génération du bulletin.
        Image logoImage = null;
        if (ecole != null && ecole.getLogo() != null && !ecole.getLogo().isBlank()) {
            try {
                logoImage = Image.getInstance(ecole.getLogo());
                logoImage.scaleToFit(60, 60);
            } catch (Exception e) {
                System.out.println("⚠️ Logo établissement illisible, ignoré : " + e.getMessage());
            }
        }

        if (logoImage != null) {
            PdfPTable enteteAvecLogo = new PdfPTable(new float[]{1f, 5f});
            enteteAvecLogo.setWidthPercentage(100);

            PdfPCell logoCell = new PdfPCell(logoImage, false);
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            enteteAvecLogo.addCell(logoCell);

            PdfPCell nomCell = new PdfPCell(new Phrase(ecoleNom, ecoleFont));
            nomCell.setBorder(Rectangle.NO_BORDER);
            nomCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            nomCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            enteteAvecLogo.addCell(nomCell);

            contenuCell.addElement(enteteAvecLogo);
        } else {
            PdfPTable ecoleBox = new PdfPTable(1);
            ecoleBox.setWidthPercentage(100);
            PdfPCell ecoleCell = new PdfPCell(new Phrase(ecoleNom, ecoleFont));
            ecoleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            ecoleCell.setPadding(8);
            ecoleBox.addCell(ecoleCell);
            contenuCell.addElement(ecoleBox);
        }

        if (!ligneAdresse.isBlank()) {
            Paragraph adresse1 = new Paragraph(ligneAdresse, adresseFont);
            adresse1.setAlignment(Element.ALIGN_CENTER);
            contenuCell.addElement(adresse1);
        }

        if (!ligneContact.isBlank()) {
            Paragraph adresse2 = new Paragraph(ligneContact, adresseFont);
            adresse2.setAlignment(Element.ALIGN_CENTER);
            contenuCell.addElement(adresse2);
        }

        contenuCell.addElement(new Paragraph(" "));

        PdfPTable titreBox = new PdfPTable(1);
        titreBox.setWidthPercentage(60);
        titreBox.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell titreCell = new PdfPCell(new Phrase("BULLETIN DE NOTES", titreFont));
        titreCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titreCell.setPadding(10);
        titreBox.addCell(titreCell);
        contenuCell.addElement(titreBox);

        contenuCell.addElement(new Paragraph(" "));

        // ================= INFOS ÉLÈVE (2 colonnes, sans bordure) =================

        String anneeScolaire = (inscription != null && inscription.getAnneeScolaire() != null)
                ? String.valueOf(inscription.getAnneeScolaire().getNom())
                : "-";
        String classeNom = (inscription != null && inscription.getClasse() != null)
                ? String.valueOf(inscription.getClasse().getNomComplet())
                : "-";
        String matricule = eleve.getMatricule() != null ? String.valueOf(eleve.getMatricule()) : "-";

        PdfPTable infos = new PdfPTable(2);
        infos.setWidthPercentage(100);
        infos.setWidths(new float[]{1f, 1f});

        infos.addCell(infoCell("Année Scolaire " + anneeScolaire, labelFont));
        infos.addCell(infoCellDroite("Nom : ", eleve.getNom(), labelFont, valeurFont));

        infos.addCell(infoCell(periode, labelFont));
        infos.addCell(infoCellDroite("Prénom : ", eleve.getPrenom(), labelFont, valeurFont));

        infos.addCell(infoCell("Classe " + classeNom, labelFont));
        infos.addCell(infoCellDroite("N°Matricule : ", matricule, labelFont, valeurFont));

        contenuCell.addElement(infos);
        contenuCell.addElement(new Paragraph(" "));

        // ================= TABLEAU DES MATIÈRES =================

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1.3f, 1.3f, 1.3f, 0.8f, 1.3f, 2.2f});

        addHeader(table, "Matières", tableHeaderFont);
        addHeader(table, "Moyen.\nClasse", tableHeaderFont);
        addHeader(table, "Notes\nCompo.", tableHeaderFont);
        addHeader(table, "Moyen.\nNotes", tableHeaderFont);
        addHeader(table, "Coef", tableHeaderFont);
        addHeader(table, "Moyen.\nCoef", tableHeaderFont);
        addHeader(table, "Appréciations\ndes Professeurs", tableHeaderFont);

        double totalGeneral = 0;
        double totalCoeff = 0;

        for (Note n : notes) {

            if (n == null) continue;

            String matiereNom = n.getMatiere() != null ? n.getMatiere().getNom() : "N/A";
            int coeff = n.getCoeff() != null ? n.getCoeff() : 1;

            if (estConduite(n)) {
                // Ligne "Conduite" : pas de moyenne calculée, une note
                // directe (stockée ici dans nExem par convention — adaptez
                // si votre modèle de données diffère).
                double noteConduite = safe(n.getNExem());
                double points = noteConduite * coeff;

                totalGeneral += points;
                totalCoeff += coeff;

                table.addCell(bodyCell(matiereNom, tableCellFont, Element.ALIGN_LEFT));
                table.addCell(bodyCell("", tableCellFont, Element.ALIGN_CENTER));
                table.addCell(bodyCell("", tableCellFont, Element.ALIGN_CENTER));
                table.addCell(bodyCell("", tableCellFont, Element.ALIGN_CENTER));
                table.addCell(bodyCell(String.valueOf(coeff), tableCellFont, Element.ALIGN_CENTER));
                table.addCell(bodyCell(formatNote(points), tableCellFont, Element.ALIGN_CENTER));
                table.addCell(bodyCell(getAppreciationMatiere(noteConduite), tableCellFont, Element.ALIGN_CENTER));
                continue;
            }

            double nClass = safe(n.getNClass());
            double nExem = safe(n.getNExem());
            double moyenne = calculerMoyenneNote(n);
            double points = moyenne * coeff;

            totalGeneral += points;
            totalCoeff += coeff;

            table.addCell(bodyCell(matiereNom, tableCellFont, Element.ALIGN_LEFT));
            table.addCell(bodyCell(formatNote(nClass), tableCellFont, Element.ALIGN_CENTER));
            table.addCell(bodyCell(formatNote(nExem), tableCellFont, Element.ALIGN_CENTER));
            table.addCell(bodyCell(formatNote(moyenne), tableCellFont, Element.ALIGN_CENTER));
            table.addCell(bodyCell(String.valueOf(coeff), tableCellFont, Element.ALIGN_CENTER));
            table.addCell(bodyCell(formatNote(points), tableCellFont, Element.ALIGN_CENTER));
            table.addCell(bodyCell(getAppreciationMatiere(moyenne), tableCellFont, Element.ALIGN_CENTER));
        }

        contenuCell.addElement(table);

        // ================= TOTAUX =================

        PdfPTable totaux = new PdfPTable(2);
        totaux.setWidthPercentage(60);
        totaux.setWidths(new float[]{1.3f, 1f});
        totaux.addCell(totalCell("Total Général :", labelFont, Element.ALIGN_LEFT));
        totaux.addCell(totalCell(formatNote(totalGeneral), valeurFont, Element.ALIGN_CENTER));
        totaux.addCell(totalCell("Total Coeff :", labelFont, Element.ALIGN_LEFT));
        totaux.addCell(totalCell(String.valueOf((int) totalCoeff), valeurFont, Element.ALIGN_CENTER));
        contenuCell.addElement(totaux);

        contenuCell.addElement(new Paragraph(" "));

        // ================= MOYENNE / RANG =================

        double moyenneGenerale = totalCoeff == 0 ? 0 : totalGeneral / totalCoeff;

        String rangTexte = "-";
        String moyenne1erTexte = "-";
        if (moyennesDeLaClasse != null && !moyennesDeLaClasse.isEmpty()) {
            int rang = calculRang(moyennesDeLaClasse, moyenneGenerale);
            rangTexte = rang + " / " + moyennesDeLaClasse.size();

            double moyenne1er = moyennesDeLaClasse.stream()
                    .max(Comparator.naturalOrder())
                    .orElse(moyenneGenerale);
            moyenne1erTexte = formatNote(moyenne1er);
        }

        PdfPTable moyenneRang = new PdfPTable(2);
        moyenneRang.setWidthPercentage(100);
        moyenneRang.addCell(boxCell("Moyenne : " + formatNote(moyenneGenerale), labelFont));
        moyenneRang.addCell(boxCell("Rang : " + rangTexte, labelFont));
        contenuCell.addElement(moyenneRang);

        contenuCell.addElement(new Paragraph(" "));

        PdfPTable moyenne1erTable = new PdfPTable(1);
        moyenne1erTable.setWidthPercentage(50);
        moyenne1erTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        moyenne1erTable.addCell(boxCell("Moyenne du 1er : " + moyenne1erTexte, labelFont));
        contenuCell.addElement(moyenne1erTable);

        contenuCell.addElement(new Paragraph(" "));

        // ================= APPRÉCIATION GÉNÉRALE =================

        PdfPTable appreciationBox = new PdfPTable(1);
        appreciationBox.setWidthPercentage(60);
        appreciationBox.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell appreciationTitre = new PdfPCell(new Phrase("APPRECIATIONS GENERALES", tableHeaderFont));
        appreciationTitre.setHorizontalAlignment(Element.ALIGN_CENTER);
        appreciationTitre.setBackgroundColor(BaseColor.LIGHT_GRAY);
        appreciationBox.addCell(appreciationTitre);

        PdfPCell appreciationValeur = new PdfPCell(new Phrase(getMention(moyenneGenerale).toUpperCase(), grandTitreFont));
        appreciationValeur.setHorizontalAlignment(Element.ALIGN_CENTER);
        appreciationValeur.setPadding(10);
        appreciationBox.addCell(appreciationValeur);

        contenuCell.addElement(appreciationBox);

        // ================= SIGNATURES =================

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

    /** Format plein page (un bulletin par page), comportement d'origine. */
    private void genererBulletinDansDocument(
            Document doc,
            List<Note> notes,
            Eleve eleve,
            String periode,
            Inscription inscription,
            List<Double> moyennesDeLaClasse,
            Ecole ecole
    ) throws DocumentException {
        doc.add(construireContenuBulletin(notes, eleve, periode, inscription, moyennesDeLaClasse, ecole, 1.0f));
    }

    // ================= HELPERS DE MISE EN FORME =================

    private String formatNote(double v) {
        return String.format("%.2f", v).replace(".00", "").replace('.', ',');
    }

    private PdfPCell addHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        table.addCell(cell);
        return cell;
    }

    private PdfPCell bodyCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell totalCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private PdfPCell boxCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell infoCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private PdfPCell infoCellDroite(String label, String valeur, Font labelFont, Font valeurFont) {
        Phrase phrase = new Phrase();
        phrase.add(new Chunk(label, labelFont));
        phrase.add(new Chunk(valeur == null ? "-" : valeur, valeurFont));
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private PdfPCell noBorder(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(0);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPaddingTop(30);
        return cell;
    }
}