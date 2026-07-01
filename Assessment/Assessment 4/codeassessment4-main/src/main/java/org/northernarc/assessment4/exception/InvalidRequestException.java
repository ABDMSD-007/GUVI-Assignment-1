package org.northernarc.assessment4.exception;

/**
 * Thrown for a semantically invalid request that is not covered by Bean Validation
 * (e.g. a negative amount passed to a service method, an unsupported filter value).
 *
 * Maps to HTTP 400 Bad Request.
 */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}

