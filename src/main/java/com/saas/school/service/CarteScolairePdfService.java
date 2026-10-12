package com.saas.school.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.saas.school.dto.CarteScolaireDto;
import com.saas.school.entity.*;
import com.saas.school.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Génère les cartes scolaires (recto) en planche A4 découpable
 * (2 colonnes x 4 lignes = 8 cartes par page).
 */
@Service
@RequiredArgsConstructor
public class CarteScolairePdfService {

    private static final String NOM_TEMPLATE = "cartes/carte-scolaire";
    private static final int CARTES_PAR_PAGE = 8;
    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    private final UtilisateurRepository utilisateurRepository;
    private final SpringTemplateEngine templateEngine;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /** URL de base pour les images stockées en chemin relatif (/uploads/...). */
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // ================================================================
    // GÉNÉRATION PDF
    // ================================================================

    /** Une seule carte. */
    public byte[] genererPdfEleve(CarteScolaireDto dto) {
        return genererPdf(List.of(dto));
    }

    /** Toutes les cartes d'une classe (liste de DTO déjà construits). */
    public byte[] genererPdfClasse(List<CarteScolaireDto> dtos) {
        return genererPdf(dtos);
    }

    private byte[] genererPdf(List<CarteScolaireDto> cartes) {

        List<List<CarteScolaireDto>> pages = decouperEnPages(cartes, CARTES_PAR_PAGE);

        Context context = new Context();
        context.setVariable("pages", pages);

        String html = templateEngine.process(NOM_TEMPLATE, context);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();

            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la génération des cartes scolaires.", e);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération des cartes scolaires.", e);
        }
    }

    private List<List<CarteScolaireDto>> decouperEnPages(List<CarteScolaireDto> cartes, int taille) {
        List<List<CarteScolaireDto>> pages = new ArrayList<>();

        for (int i = 0; i < cartes.size(); i += taille) {
            pages.add(cartes.subList(i, Math.min(i + taille, cartes.size())));
        }

        if (pages.isEmpty()) {
            pages.add(List.of());
        }

        return pages;
    }

    // ================================================================
    // CONSTRUCTION DU DTO
    // ================================================================

    /** Version simple : une seule carte (cache jetable). */
    public CarteScolaireDto construireDto(Inscription inscription) {
        return construireDto(inscription, new HashMap<>());
    }

    /**
     * Version avec cache partagé : le logo de l'école et le nom du
     * directeur ne sont récupérés qu'une seule fois pour toute la classe.
     */
    public CarteScolaireDto construireDto(Inscription inscription, Map<String, String> cache) {

        Eleve eleve = inscription.getEleve();
        Ecole ecole = inscription.getEcole();
        AnneeScolaire annee = inscription.getAnneeScolaire();

        // ---------- Initiales ----------
        String initiales = "EL";
        if (eleve != null) {
            String p = eleve.getPrenom() != null && !eleve.getPrenom().isBlank()
                    ? eleve.getPrenom().trim().substring(0, 1) : "";
            String n = eleve.getNom() != null && !eleve.getNom().isBlank()
                    ? eleve.getNom().trim().substring(0, 1) : "";
            String calc = (p + n).toUpperCase(Locale.FRENCH);
            if (!calc.isBlank()) {
                initiales = calc;
            }
        }

        // ---------- Base ----------
        CarteScolaireDto.CarteScolaireDtoBuilder builder = CarteScolaireDto.builder()
                .inscriptionId(inscription.getId())
                .classeNom(inscription.getClasse() != null
                        ? inscription.getClasse().getNomComplet()
                        : null)
                .anneeScolaireNom(annee != null ? annee.getNom() : null)
                .validiteDebut(formatterDate(annee != null ? annee.getDateDebut() : null))
                .validiteFin(formatterDate(annee != null ? annee.getDateFin() : null))
                .initiales(initiales);

        // ---------- Élève ----------
        if (eleve != null) {
            builder.nom(eleve.getNom())
                    .prenom(eleve.getPrenom())
                    .matricule(eleve.getMatricule())
                    .dateNaissanceFormatee(eleve.getDateNaissance() != null
                            ? eleve.getDateNaissance().format(FORMAT_DATE)
                            : null)
                    .lieuNaissance(eleve.getLieuNaissance())
                    .groupeSanguin(eleve.getGroupeSanguin())
                    .telephoneParent(eleve.getTelephoneTuteur())
                    .photoBase64(telechargerEnBase64(eleve.getPhotoUrl()));
        }

        // ---------- École (avec cache) ----------
        if (ecole != null) {
            builder.ecoleNom(ecole.getNom())
                    .ecoleAdresse(ecole.getAdresse())
                    .ecoleVille(ecole.getVille())
                    .ecoleTelephone(ecole.getTelephone());

            if (ecole.getLogo() != null && !ecole.getLogo().isBlank()) {
                builder.logoBase64(
                        cache.computeIfAbsent("img:" + ecole.getLogo(),
                                k -> telechargerEnBase64(ecole.getLogo())));
            }

            if (ecole.getId() != null) {
                builder.directeurNom(
                        cache.computeIfAbsent("dir:" + ecole.getId(),
                                k -> utilisateurRepository
                                        .findFirstByEcoleIdAndRoleNom(ecole.getId(), "DIRECTEUR")
                                        .map(Utilisateur::getNom)
                                        .orElse(null)));
            }
        }

        return builder.build();
    }

    private String formatterDate(LocalDate date) {
        return date == null ? "?" : date.format(FORMAT_DATE);
    }

    // ================================================================
    // IMAGES -> BASE64
    // ================================================================

    /**
     * Télécharge une image (photo élève ou logo) et la convertit en
     * data URI base64. Retourne null si l'URL est vide ou en cas d'échec
     * (le template affiche alors un repli).
     */
    private String telechargerEnBase64(String url) {

        if (url == null || url.isBlank()) {
            return null;
        }

        try {
            String urlComplete = url.trim();

            // Anciennes images locales : /uploads/logo.png
            if (urlComplete.startsWith("/")) {
                urlComplete = baseUrl + urlComplete;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlComplete))
                    .header("User-Agent", "DaniSchool-PDF")
                    .GET()
                    .build();

            HttpResponse<byte[]> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                System.err.println("Image inaccessible : " + urlComplete
                        + " | HTTP " + response.statusCode());
                return null;
            }

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse(null);

            if (contentType == null || !contentType.startsWith("image/")) {
                contentType = "image/jpeg";
            }

            return "data:" + contentType + ";base64,"
                    + Base64.getEncoder().encodeToString(response.body());

        } catch (Exception e) {
            System.err.println("Erreur téléchargement image : " + url + " | " + e.getMessage());
            return null;
        }
    }
}