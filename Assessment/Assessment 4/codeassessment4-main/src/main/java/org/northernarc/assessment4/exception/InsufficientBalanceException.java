package org.northernarc.assessment4.exception;

/**
 * Thrown when an account does not hold enough balance to complete an operation
 * (e.g. a withdrawal or transfer larger than the available balance).
 *
 * Maps to HTTP 400 Bad Request.
 */
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

