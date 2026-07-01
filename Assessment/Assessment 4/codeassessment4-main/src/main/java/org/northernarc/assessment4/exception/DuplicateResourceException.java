package org.northernarc.assessment4.exception;

/**
 * Thrown when a request would create a resource that already exists
 * (e.g. a Customer with an email that is already registered).
 *
 * Maps to HTTP 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}

