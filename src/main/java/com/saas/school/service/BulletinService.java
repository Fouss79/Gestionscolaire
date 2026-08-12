package com.saas.school.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.saas.school.dto.BulletinDTO;
import com.saas.school.entity.Eleve;
import com.saas.school.entity.Inscription;
import com.saas.school.entity.Note;
import com.saas.school.repository.InscriptionRepository;
import com.saas.school.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BulletinService {
    private final NoteRepository noteRepository;

   private  final InscriptionRepository inscriptionRepository;
    public byte[] generateBulletin(List<Note> notes, Eleve eleve, String periode) {

        System.out.println("🚀 START PDF GENERATION");


        try {
            if (notes == null || notes.isEmpty()) {
                throw new RuntimeException("❌ Aucune note trouvée");
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
            PdfPTable table = new PdfPTable(6); // ✅ 6 colonnes corrigé
            table.setWidthPercentage(100);

            // 🔥 HEADERS
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
                Integer coeff = n.getCoeff();

                // 🔥 moyenne correcte
                double moyenne = (nClass + (nExem * 2)) / 3;

                double points = moyenne * coeff;

                totalPoints += points;
                totalCoeff += coeff;

                // 🔥 LOG DEBUG
                System.out.println("➡️ " + matiere +
                        " | M=" + moyenne +
                        " | Coeff=" + coeff);

                // 🔥 ADD ROW
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

            // 🔥 DEBUG FILE
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
        Inscription inscription = inscriptionRepository.findById(inscriptionId).orElseThrow(() -> new RuntimeException("Inscription introuvable"));
        List<Note> notes = noteRepository
                .findByEleveIdAndClasseIdAndAnneeScolaireId(inscription.getEleve().getId(), classeId, anneeId);

        double total = 0;
        double coeffTotal = 0;

        for (Note n : notes) {

            double moyenne = (n.getNClass() + n.getNExem()*2) / 3;
            double coeff = n.getCoeff() == null ? 1 : n.getCoeff();

            total += moyenne * coeff;
            coeffTotal += coeff;
        }

        double moyenneAnnuelle = coeffTotal == 0 ? 0 : total / coeffTotal;

        BulletinDTO dto = new BulletinDTO();
        dto.setEleveId(inscription.getEleve().getId());
        dto.setMoyenneAnnuelle(moyenneAnnuelle);

        // 🎯 mention automatique
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