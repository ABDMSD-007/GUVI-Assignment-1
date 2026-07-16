package com.example.demo.exception;

/**
 * Thrown when attempting to register a customer with an email that already exists.
 * Mapped to HTTP 409 (Conflict) by {@link GlobalExceptionHandler}.
 */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String message) {
        super(message);
    }
}

