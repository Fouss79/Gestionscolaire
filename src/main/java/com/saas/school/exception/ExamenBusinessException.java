package com.saas.school.exception;

/**
 * Exception métier pour le module Examens (règles de validation :
 * cohérence école/année scolaire, capacité de salle, conflits de créneau...).
 * Adapter pour étendre une éventuelle exception de base du projet si elle existe déjà.
 */
public class ExamenBusinessException extends RuntimeException {

    public ExamenBusinessException(String message) {
        super(message);
    }
}
