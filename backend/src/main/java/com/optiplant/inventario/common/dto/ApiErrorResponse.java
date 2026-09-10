package com.optiplant.inventario.common.dto;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Formato de error estándar de la API (Sección 5 del roadmap). El campo
 * {@code message} siempre es específico al caso, nunca genérico.
 */
public record ApiErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path
) {

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(ISO_UTC.format(Instant.now()), status, error, message, path);
    }
}
