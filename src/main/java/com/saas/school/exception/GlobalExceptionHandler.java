package com.saas.school.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * =========================================================
 * 🌐 GESTIONNAIRE GLOBAL DES EXCEPTIONS
 * =========================================================
 *
 * Objectif : renvoyer TOUJOURS un JSON de forme prévisible
 * au frontend, avec le bon code HTTP, au lieu de laisser
 * Spring Boot renvoyer sa page d'erreur par défaut.
 *
 * Format de réponse :
 * {
 *   "timestamp": "...",
 *   "status": 409,
 *   "message": "Conflit : cet enseignant a déjà un cours sur ce créneau"
 * }
 *
 * Le frontend (extraireMessageErreur) lit directement
 * error.response.data.message.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * =====================================================
     * 🚫 CONFLITS MÉTIER (RuntimeException "métier")
     * =====================================================
     *
     * Le service lève des RuntimeException avec des messages
     * préfixés selon leur nature :
     *   - "Conflit : ..."          → 409 CONFLICT
     *   - "Heures insuffisantes"   → 409 CONFLICT (quota dépassé)
     *   - "... introuvable"        → 404 NOT_FOUND
     *   - tout le reste            → 400 BAD_REQUEST
     *
     * ⚠️ Si un jour tu remplaces ces RuntimeException par des
     * exceptions dédiées (ConflitCreneauException, EntiteIntrouvableException, ...),
     * ce handler pourra être simplifié en plusieurs @ExceptionHandler
     * distincts, plus robustes que ce filtrage par préfixe de message.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {

        String message = ex.getMessage() != null
                ? ex.getMessage()
                : "Une erreur est survenue.";

        HttpStatus status = determinerStatus(message);

        return ResponseEntity
                .status(status)
                .body(corpsErreur(status, message));
    }

    private HttpStatus determinerStatus(String message) {

        String messageMinuscule = message.toLowerCase();

        if (messageMinuscule.startsWith("conflit")
                || messageMinuscule.contains("heures insuffisantes")
                || messageMinuscule.contains("déjà planifiées")) {
            return HttpStatus.CONFLICT; // 409
        }

        if (messageMinuscule.contains("introuvable")) {
            return HttpStatus.NOT_FOUND; // 404
        }

        return HttpStatus.BAD_REQUEST; // 400
    }


    /**
     * =====================================================
     * ✅ ERREURS DE VALIDATION (@Valid sur les DTO)
     * =====================================================
     *
     * Déclenché quand un champ annoté @NotNull, @Min, @NotBlank...
     * échoue dans un DTO reçu via @RequestBody @Valid.
     *
     * Renvoie le détail champ par champ pour faciliter le debug
     * côté frontend.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {

        String messageGlobal = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formaterErreurChamp)
                .collect(Collectors.joining(" · "));

        Map<String, Object> body = corpsErreur(
                HttpStatus.BAD_REQUEST,
                messageGlobal.isEmpty()
                        ? "Données invalides."
                        : messageGlobal
        );

        // Détail structuré, utile si le frontend veut surligner
        // un champ précis plus tard.
        body.put(
                "errors",
                ex.getBindingResult().getFieldErrors().stream()
                        .collect(Collectors.toMap(
                                FieldError::getField,
                                fe -> fe.getDefaultMessage() != null
                                        ? fe.getDefaultMessage()
                                        : "Valeur invalide",
                                (a, b) -> a // en cas de doublon de champ, garder le premier
                        ))
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    private String formaterErreurChamp(FieldError fieldError) {
        return fieldError.getField() + " : " + fieldError.getDefaultMessage();
    }


    /**
     * =====================================================
     * 🛑 FILET DE SÉCURITÉ - TOUT LE RESTE
     * =====================================================
     *
     * Attrape les exceptions non prévues (NullPointerException,
     * erreurs SQL non gérées, etc.) pour ne JAMAIS renvoyer
     * de page d'erreur HTML/stacktrace brute au frontend.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleExceptionGenerique(Exception ex) {

        // Toujours logguer côté serveur pour investigation,
        // même si le message renvoyé au client reste générique.
        ex.printStackTrace();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(corpsErreur(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Erreur interne du serveur. Veuillez réessayer."
                ));
    }


    // =========================================================
    // 🧱 CONSTRUCTION DU CORPS DE RÉPONSE
    // =========================================================

    private Map<String, Object> corpsErreur(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}