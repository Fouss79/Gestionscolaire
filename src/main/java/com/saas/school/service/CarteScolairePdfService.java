package com.saas.school.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.saas.school.dto.CarteScolaireDto;
import com.saas.school.entity.*;
import com.saas.school.repository.InscriptionRepository;
import com.saas.school.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.time.format.TextStyle;
import java.util.*;

/**
 * Génère les cartes scolaires (recto), en planche A4 imprimable et
 * découpable (2 colonnes x 4 lignes = 8 cartes par page), sur le même
 * principe que ResultatFinAnneePdfService : Thymeleaf pour le HTML,
 * openhtmltopdf pour la conversion en PDF.
 */
@Service
@RequiredArgsConstructor
public class CarteScolairePdfService {

    private static final String NOM_TEMPLATE = "cartes/carte-scolaire";
    private static final int CARTES_PAR_PAGE = 8;
    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private final UtilisateurRepository utilisateurRepository;
    private final InscriptionRepository inscriptionRepository;

    private final SpringTemplateEngine templateEngine;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ================================================================
    // GÉNÉRATION PDF
    // ================================================================

    public byte[] genererPdfEleve(CarteScolaireDto dto) {
        return genererPdf(List.of(dto));
    }

    @Transactional(readOnly = true)
    public byte[] genererPdfClasse(Long classeId, Long anneeScolaireId) {

        List<Inscription> inscriptions =
                inscriptionRepository.findActivesByClasseAndAnnee(classeId, anneeScolaireId);

        if (inscriptions.isEmpty()) {
            throw new IllegalStateException("Aucun élève inscrit dans cette classe.");
        }

        // Cache par génération : le logo n'est téléchargé qu'une seule fois
        Map<String, String> cacheImages = new HashMap<>();

        List<CarteScolaireDto> dtos = inscriptions.stream()
                .map(i -> construireDto(i, cacheImages))
                .toList();

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
    // CONSTRUCTION DU DTO À PARTIR D'UNE INSCRIPTION
    // ================================================================

    /**
     * Construit le DTO d'une carte à partir d'une inscription active.
     * inscription.getClasse(), inscription.getEleve(),
     * inscription.getAnneeScolaire() et inscription.getEcole() sont
     * supposés déjà chargés (JOIN FETCH ou lazy dans une transaction).
     */
    /**
     * Version simple : une seule carte (cache local jetable).
     */
    public CarteScolaireDto construireDto(Inscription inscription) {
        return construireDto(inscription, new HashMap<>());
    }

    /**
     * Version avec cache partagé : le logo de l'école et le nom du directeur
     * ne sont récupérés qu'une seule fois pour toute la classe.
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

        // ---------- Builder de base ----------
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

            // Logo : un seul téléchargement par génération
            if (ecole.getLogo() != null && !ecole.getLogo().isBlank()) {
                builder.logoBase64(
                        cache.computeIfAbsent("img:" + ecole.getLogo(),
                                k -> telechargerEnBase64(ecole.getLogo())));
            }

            // Directeur : une seule requête par école
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
        if (date == null) {
            return "?";
        }
        return date.format(FORMAT_DATE);
    }

    /**
     * Télécharge une image (photo élève ou logo école) et la convertit
     * en data URI base64, pour un rendu PDF fiable et hors-ligne
     * (openhtmltopdf ne va pas systématiquement re-télécharger une
     * image distante au moment du rendu). Retourne null si l'URL est
     * vide ou si le téléchargement échoue (le template affiche alors
     * un repli : initiales ou icône générique).
     */
    private String telechargerEnBase64(String url) {

        if (url == null || url.isBlank()) {
            return null;
        }

        try {

            String urlComplete = url.trim();

            /*
             * ============================================================
             * ANCIENNES IMAGES LOCALES
             * ============================================================
             *
             * Exemple :
             * /uploads/logo.png
             */
            if (urlComplete.startsWith("/")) {

                urlComplete =
                        "http://localhost:8080" + urlComplete;
            }

            System.out.println(
                    "===================================="
            );

            System.out.println(
                    "Téléchargement image PDF : "
                            + urlComplete
            );

            /*
             * ============================================================
             * REQUÊTE HTTP
             * ============================================================
             */

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(urlComplete))
                            .header(
                                    "User-Agent",
                                    "DaniSchool-PDF"
                            )
                            .GET()
                            .build();

            HttpResponse<byte[]> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofByteArray()
                    );

            System.out.println(
                    "Statut image : "
                            + response.statusCode()
            );

            /*
             * ============================================================
             * VÉRIFICATION
             * ============================================================
             */

            if (response.statusCode() != 200) {

                System.err.println(
                        "Impossible de télécharger l'image : "
                                + urlComplete
                                + " | HTTP "
                                + response.statusCode()
                );

                return null;
            }

            /*
             * ============================================================
             * TYPE MIME
             * ============================================================
             */

            String contentType =
                    response.headers()
                            .firstValue("Content-Type")
                            .orElse(null);

            /*
             * Certains serveurs peuvent ne pas retourner
             * correctement le Content-Type.
             */
            if (contentType == null
                    || contentType.isBlank()
                    || !contentType.startsWith("image/")) {

                contentType = "image/jpeg";
            }

            /*
             * ============================================================
             * BASE64
             * ============================================================
             */

            String base64 =
                    Base64.getEncoder()
                            .encodeToString(response.body());

            String dataUri =
                    "data:"
                            + contentType
                            + ";base64,"
                            + base64;

            System.out.println(
                    "Image convertie en Base64 : OK"
            );

            System.out.println(
                    "===================================="
            );

            return dataUri;

        } catch (Exception e) {

            System.err.println(
                    "Erreur téléchargement image : "
                            + url
            );

            e.printStackTrace();

            return null;
        }
    }
}