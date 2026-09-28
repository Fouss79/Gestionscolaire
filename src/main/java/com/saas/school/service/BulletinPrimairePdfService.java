
package com.saas.school.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.saas.school.dto.BulletinDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BulletinPrimairePdfService {

    private final BulletinPrimaireService bulletinService;
    private final TemplateEngine templateEngine;

    /**
     * PDF de toute une classe :
     * une page A5 par élève.
     */
    @Transactional(readOnly = true)
    public byte[] genererClasse(
            Long classeId,
            Long anneeId,
            String mois
    ) {
        List<BulletinDtos> bulletins =
                bulletinService.construire(classeId, anneeId, mois);

        if (bulletins.isEmpty()) {
            throw new IllegalStateException(
                    "Aucun bulletin disponible pour cette classe."
            );
        }

        return genererPdf(bulletins);
    }

    /**
     * PDF individuel d'un élève du primaire.
     */
    @Transactional(readOnly = true)
    public byte[] genererEleve(
            Long inscriptionId,
            Long classeId,
            Long anneeId,
            String mois
    ) {
        BulletinDtos bulletin =
                bulletinService
                        .construire(classeId, anneeId, mois)
                        .stream()
                        .filter(b ->
                                inscriptionId.equals(b.inscriptionId())
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Bulletin introuvable pour l'inscription "
                                                + inscriptionId
                                )
                        );

        return genererPdf(List.of(bulletin));
    }

    /**
     * Rendu PDF commun aux deux méthodes.
     */
    private byte[] genererPdf(List<BulletinDtos> bulletins) {

        Context context = new Context();
        context.setVariable("bulletins", bulletins);

        String html = templateEngine.process(
                "bulletin-primaire",
                context
        );

        try (ByteArrayOutputStream out =
                     new ByteArrayOutputStream()) {

            PdfRendererBuilder builder =
                    new PdfRendererBuilder();

            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();

            return out.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Erreur lors de la génération du PDF primaire",
                    e
            );
        }
    }
}