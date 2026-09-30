package com.saas.school.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.saas.school.entity.AnneeScolaire;
import com.saas.school.entity.Ecole;
import com.saas.school.entity.Eleve;
import com.saas.school.entity.Inscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CertificatScolaritePdfService {

    private static final String NOM_TEMPLATE =
            "certificats/certificat-scolarite";

    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy",
                    Locale.FRENCH
            );

    private final SpringTemplateEngine templateEngine;

    /**
     * Génère le certificat de scolarité d'une inscription.
     */
    public byte[] genererCertificat(Inscription inscription) {

        if (inscription == null) {
            throw new IllegalArgumentException(
                    "L'inscription est obligatoire."
            );
        }

        Eleve eleve = inscription.getEleve();
        Ecole ecole = inscription.getEcole();
        AnneeScolaire annee = inscription.getAnneeScolaire();

        if (eleve == null) {
            throw new IllegalArgumentException(
                    "Aucun élève associé à cette inscription."
            );
        }

        if (ecole == null) {
            throw new IllegalArgumentException(
                    "Aucune école associée à cette inscription."
            );
        }

        if (annee == null) {
            throw new IllegalArgumentException(
                    "Aucune année scolaire associée à cette inscription."
            );
        }

        Context context = new Context();

        context.setVariable(
                "eleve",
                eleve
        );

        context.setVariable(
                "ecole",
                ecole
        );

        context.setVariable(
                "annee",
                annee
        );

        context.setVariable(
                "inscription",
                inscription
        );

        context.setVariable(
                "dateEdition",
                LocalDate.now().format(FORMAT_DATE)
        );

        context.setVariable(
                "dateNaissance",
                formatterDate(eleve.getDateNaissance())
        );

        context.setVariable(
                "dateDebut",
                formatterDate(annee.getDateDebut())
        );

        context.setVariable(
                "dateFin",
                formatterDate(annee.getDateFin())
        );

        String html =
                templateEngine.process(
                        NOM_TEMPLATE,
                        context
                );

        try (
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {

            PdfRendererBuilder builder =
                    new PdfRendererBuilder();

            builder.useFastMode();

            builder.withHtmlContent(
                    html,
                    null
            );

            builder.toStream(output);

            builder.run();

            return output.toByteArray();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Erreur lors de la génération du certificat de scolarité.",
                    e
            );
        }
    }

    private String formatterDate(LocalDate date) {

        if (date == null) {
            return "";
        }

        return date.format(FORMAT_DATE);
    }
}