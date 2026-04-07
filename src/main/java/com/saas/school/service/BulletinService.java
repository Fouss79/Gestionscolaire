package com.saas.school.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.saas.school.entity.Eleve;
import com.saas.school.entity.Note;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BulletinService {

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
                double coeff = safe(n.getCoeff());

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

}