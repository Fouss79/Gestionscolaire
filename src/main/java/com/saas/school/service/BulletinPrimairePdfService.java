package com.saas.school.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.saas.school.dto.BulletinDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BulletinPrimairePdfService {

    private final BulletinPrimaireService bulletinService;
    private final TemplateEngine templateEngine;

    /** Un PDF A5 avec une page par élève de la classe. */
    public byte[] genererClasse(Long classeId, Long anneeId, String mois) {
        List<BulletinDtos> bulletins = bulletinService.construire(classeId, anneeId, mois);
        if (bulletins.isEmpty()) {
            throw new IllegalStateException("Aucun élève actif dans cette classe.");
        }

        Context ctx = new Context();
        ctx.setVariable("bulletins", bulletins);
        String html = templateEngine.process("bulletin-primaire", ctx);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la génération du PDF", e);
        }
    }
}
