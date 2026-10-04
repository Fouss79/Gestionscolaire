package com.saas.school.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.secret-key}")
    private String supabaseSecretKey;

    @Value("${supabase.bucket}")
    private String bucket;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    public String uploadImage(MultipartFile file, String folder) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Aucune image reçue.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException(
                    "L'image ne doit pas dépasser 5 Mo."
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                (!contentType.equalsIgnoreCase("image/jpeg")
                        && !contentType.equalsIgnoreCase("image/png")
                        && !contentType.equalsIgnoreCase("image/webp"))) {

            throw new RuntimeException(
                    "Format d'image non autorisé. Utilisez JPG, PNG ou WEBP."
            );
        }

        String extension = getExtension(contentType);

        String fileName =
                UUID.randomUUID() + extension;

        String path =
                folder + "/" + fileName;

        String uploadUrl =
                supabaseUrl
                        + "/storage/v1/object/"
                        + bucket
                        + "/"
                        + path;

        try {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Authorization", "Bearer " + supabaseSecretKey)
                    .header("apikey", supabaseSecretKey)
                    .header("Content-Type", contentType)
                    .header("x-upsert", "false")
                    .PUT(
                            HttpRequest.BodyPublishers.ofByteArray(
                                    file.getBytes()
                            )
                    )
                    .build();

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {

                throw new RuntimeException(
                        "Erreur Supabase Storage : "
                                + response.statusCode()
                                + " - "
                                + response.body()
                );
            }

            return supabaseUrl
                    + "/storage/v1/object/public/"
                    + bucket
                    + "/"
                    + path;

        } catch (IOException | InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Impossible d'envoyer l'image vers Supabase.",
                    e
            );
        }
    }

    private String getExtension(String contentType) {

        return switch (contentType.toLowerCase()) {

            case "image/jpeg" -> ".jpg";

            case "image/png" -> ".png";

            case "image/webp" -> ".webp";

            default -> throw new RuntimeException(
                    "Type d'image non supporté."
            );
        };
    }
}