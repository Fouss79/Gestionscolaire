package com.saas.school.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.saas.school.dto.ResultatFinAnneeDto;
import com.saas.school.entity.DecisionConseil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResultatFinAnneePdfService {

    private static final String NOM_TEMPLATE =
            "resultats/resultat-fin-annee";

    private final SpringTemplateEngine templateEngine;

    /**
     * PDF individuel
     */
    public byte[] genererPdfEleve(ResultatFinAnneeDto dto) {
        return genererPdf(List.of(dto));
    }

    /**
     * PDF pour toute une classe
     */
    public byte[] genererPdfClasse(List<ResultatFinAnneeDto> dtos) {
        return genererPdf(dtos);
    }

    /**
     * Génération du PDF
     */
    private byte[] genererPdf(List<ResultatFinAnneeDto> resultats) {

        System.out.println("===== PDF RESULTATS =====");
        System.out.println(
                "Nombre de résultats reçus : " +
                        (resultats == null ? "NULL" : resultats.size())
        );

        if (resultats != null && !resultats.isEmpty()) {

            ResultatFinAnneeDto dto = resultats.get(0);

            System.out.println(
                    "Nom : " + dto.getNomEtPrenom()
            );

            System.out.println(
                    "Classe : " + dto.getClasseNom()
            );

            System.out.println(
                    "Moyenne : " + dto.getMoyenneFormatee()
            );
        }

        // Sécurité : éviter de générer un PDF vide
        if (resultats == null || resultats.isEmpty()) {
            throw new IllegalArgumentException(
                    "Aucun résultat de fin d'année à exporter."
            );
        }

        String html = genererHtml(resultats);

        System.out.println(
                "Longueur HTML généré : " +
                        (html == null ? "NULL" : html.length())
        );

        if (html == null || html.isBlank()) {
            throw new IllegalStateException(
                    "Le HTML du bulletin de fin d'année est vide."
            );
        }

        try (ByteArrayOutputStream os =
                     new ByteArrayOutputStream()) {

            PdfRendererBuilder builder =
                    new PdfRendererBuilder();

            builder.useFastMode();

            builder.withHtmlContent(
                    html,
                    null
            );

            builder.toStream(os);

            builder.run();

            return os.toByteArray();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erreur lors de la génération du PDF de résultats.",
                    e
            );
        }
    }

    /**
     * Génération du HTML avec Thymeleaf
     */
    private String genererHtml(
            List<ResultatFinAnneeDto> resultats) {

        Context context = new Context();

        context.setVariable(
                "resultats",
                resultats
        );

        context.setVariable(
                "toutesDecisions",
                Arrays.asList(
                        DecisionConseil.values()
                )
        );

        return templateEngine.process(
                NOM_TEMPLATE,
                context
        );
    }
}
