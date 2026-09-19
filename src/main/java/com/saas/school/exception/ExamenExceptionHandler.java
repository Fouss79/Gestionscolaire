package com.saas.school.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * NOTE : si le projet possède déjà un @RestControllerAdvice global, fusionner
 * cette méthode dedans plutôt que d'avoir deux advices qui pourraient se chevaucher.
 */
@RestControllerAdvice
public class ExamenExceptionHandler {

    @ExceptionHandler(ExamenBusinessException.class)
    public ResponseEntity<Map<String, Object>> handle(ExamenBusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Erreur métier",
                "message", ex.getMessage()
        ));
    }
}
