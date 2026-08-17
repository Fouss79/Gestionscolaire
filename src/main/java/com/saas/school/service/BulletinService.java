package com.saas.school.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.saas.school.dto.BulletinDTO;
import com.saas.school.entity.AffectationEnseignant;
import com.saas.school.entity.CoefficientMatiere;
import com.saas.school.entity.Eleve;
import com.saas.school.entity.Inscription;
import com.saas.school.entity.Note;
import com.saas.school.repository.AffectationEnseignantRepository;
import com.saas.school.repository.InscriptionRepository;
import com.saas.school.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BulletinService {
    private final NoteRepository noteRepository;
    private final InscriptionRepository inscriptionRepository;
    // FIX: injected so we can build the bulletin from what's actually
    // affected to the class (AffectationEnseignant) instead of only from
    // rows that already exist in Note.
    private final AffectationEnseignantRepository affectationEnseignantRepository;

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

    public byte[] generateBulletin(List<Note> notes, Eleve eleve, String periode) {

        System.out.println("🚀 START PDF GENERATION");

        try {
            if (notes == null || notes.isEmpty()) {
                throw new RuntimeException("❌ Aucune matière programmée pour cette classe");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Document doc = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(doc, out);

            doc.open();

            // ================= FONTS =================
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 11);

            // ================= HEADER =================
            Paragraph title = new Paragraph("BULLETIN DE NOTES", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            doc.add(new Paragraph(" "));

            doc.add(new Paragraph("Élève : " + eleve.getNom() + " " + eleve.getPrenom(), normalFont));
            doc.add(new Paragraph("Période : " + periode, normalFont));

            doc.add(new Paragraph(" "));

            // ================= TABLE =================
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);

            addHeader(table, "Matière");
            addHeader(table, "Classe");
            addHeader(table, "Examen");
            addHeader(table, "Moyenne");
            addHeader(table, "Coeff");
            addHeader(table, "Points");

            double totalPoints = 0;
            double totalCoeff = 0;

            for (Note n : notes) {

                if (n == null) continue;

                String matiere = (n.getMatiere() != null) ? n.getMatiere().getNom() : "N/A";

                double nClass = safe(n.getNClass());
                double nExem = safe(n.getNExem());
                Integer coeff = n.getCoeff() != null ? n.getCoeff() : 1;

                double moyenne = (nClass + (nExem * 2)) / 3;
                double points = moyenne * coeff;

                totalPoints += points;
                totalCoeff += coeff;

                System.out.println("➡️ " + matiere + " | M=" + moyenne + " | Coeff=" + coeff);

                table.addCell(cell(matiere));
                table.addCell(center(String.valueOf(nClass)));
                table.addCell(center(String.valueOf(nExem)));
                table.addCell(center(String.format("%.2f", moyenne)));
                table.addCell(center(String.valueOf(coeff)));
                table.addCell(center(String.format("%.2f", points)));
            }

            doc.add(table);

            // ================= MOYENNE GENERALE =================
            double moyenneGenerale = totalCoeff == 0 ? 0 : totalPoints / totalCoeff;

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Moyenne Générale : " + String.format("%.2f", moyenneGenerale), headerFont));

            // ================= FOOTER =================
            doc.add(new Paragraph(" "));
            PdfPTable sign = new PdfPTable(2);
            sign.setWidthPercentage(100);

            sign.addCell(noBorder("Le Proviseur"));
            sign.addCell(noBorder("Le Parent"));

            doc.add(sign);

            // ================= CLOSE =================
            doc.close();
            writer.close();

            byte[] pdfBytes = out.toByteArray();

            System.out.println("📦 PDF SIZE = " + pdfBytes.length);

            String path = System.getProperty("user.home") + "/bulletin_debug.pdf";
            Files.write(Paths.get(path), pdfBytes);

            System.out.println("💾 PDF SAVED: " + path);

            return pdfBytes;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur PDF: " + e.getMessage());
        }
    }

    private double safe(Double v) {
        return v == null ? 0.0 : v;
    }

    private PdfPCell addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        return cell;
    }

    private PdfPCell center(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell cell(String text) {
        return new PdfPCell(new Phrase(text));
    }

    private PdfPCell noBorder(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBorder(0);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
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

            double moyenne = (safe(n.getNClass()) + safe(n.getNExem()) * 2) / 3;
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

    public int calculRang(List<Double> moyennes, double eleveMoyenne) {
        return (int) moyennes.stream()
                .filter(m -> m > eleveMoyenne)
                .count() + 1;
    }
}