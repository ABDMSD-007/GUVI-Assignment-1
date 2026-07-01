package org.northernarc.assessment4.exception;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thrown when domain / business validation fails (as opposed to Bean Validation
 * which is triggered automatically by {@code @Valid}).
 *
 * It can optionally carry per-field messages so the {@link GlobalExceptionHandler}
 * can render the same {@code errors[]} array shape used for {@code @Valid} failures.
 *
 * Maps to HTTP 400 Bad Request.
 */
public class ValidationFailedException extends RuntimeException {

    private final Map<String, String> fieldErrors = new LinkedHashMap<>();

    public ValidationFailedException(String message) {
        super(message);
    }

    public ValidationFailedException(String message, Map<String, String> fieldErrors) {
        super(message);
        if (fieldErrors != null) {
            this.fieldErrors.putAll(fieldErrors);
        }
    }

    /** Fluent helper to accumulate field-level errors. */
    public ValidationFailedException addError(String field, String message) {
        this.fieldErrors.put(field, message);
        return this;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}

