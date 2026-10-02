package com.saas.school.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.saas.school.dto.BulletinDtos;
import com.saas.school.repository.ClasseRepository;
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
    private final ClasseRepository classeRepository;

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

        return genererPdf(bulletins, trouverNomEcole(classeId));
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

        return genererPdf(List.of(bulletin), trouverNomEcole(classeId));
    }

    /**
     * Retrouve le nom de l'école à partir de la classe.
     * Doit rester appelée dans une méthode transactionnelle
     * (relation ecole potentiellement LAZY).
     */
    private String trouverNomEcole(Long classeId) {
        return classeRepository.findById(classeId)
                .map(c -> c.getEcole().getNom())
                .orElse("");
    }

    /**
     * Rendu PDF commun aux deux méthodes.
     */
    private byte[] genererPdf(List<BulletinDtos> bulletins,
                              String nomEtablissement) {

        Context context = new Context();
        context.setVariable("bulletins", bulletins);
        context.setVariable("nomEtablissement", nomEtablissement);

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