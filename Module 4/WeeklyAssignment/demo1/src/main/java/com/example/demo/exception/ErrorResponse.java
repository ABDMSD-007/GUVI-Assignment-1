package com.example.demo.exception;

import java.time.LocalDateTime;

/**
 * Typed, structured error payload returned by {@link GlobalExceptionHandler}.
 * Replaces the previously untyped {@code Map<String, Object>} response body.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String message,
        String path
) {
}

