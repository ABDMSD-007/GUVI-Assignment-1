package com.example.demo.exception;

/**
 * Thrown when a loan operation is not allowed in the loan's current state
 * (e.g. foreclosing a loan that still has pending/missed EMIs).
 * Mapped to HTTP 400 (Bad Request) by {@link GlobalExceptionHandler}.
 */
public class InvalidLoanOperationException extends RuntimeException {
    public InvalidLoanOperationException(String message) {
        super(message);
    }
}

