package org.northernarc.assessment4.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Task 10: Centralised, best-practice exception handling.
 *
 * Every response body keeps a stable shape:
 *   timestamp, status (enum name, e.g. "BAD_REQUEST"), statusCode (int),
 *   error (reason phrase), message, path  (+ errors[] for validation failures)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---- 404 NOT FOUND ----
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCustomerNotFound(CustomerNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotFound(AccountNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    // ---- 400 BAD REQUEST : @Valid request-body validation ----
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> errors = new ArrayList<>();
        for (FieldError e : ex.getBindingResult().getFieldErrors()) {
            errors.add(e.getField() + ": " + e.getDefaultMessage());
        }
        for (ObjectError e : ex.getBindingResult().getGlobalErrors()) {
            errors.add(e.getObjectName() + ": " + e.getDefaultMessage());
        }
        return buildWithErrors(HttpStatus.BAD_REQUEST, "Validation failed", errors, req);
    }

    // ---- 400 BAD REQUEST : @ModelAttribute / form binding validation ----
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBind(BindException ex, HttpServletRequest req) {
        List<String> errors = new ArrayList<>();
        for (FieldError e : ex.getBindingResult().getFieldErrors()) {
            errors.add(e.getField() + ": " + e.getDefaultMessage());
        }
        for (ObjectError e : ex.getBindingResult().getGlobalErrors()) {
            errors.add(e.getObjectName() + ": " + e.getDefaultMessage());
        }
        return buildWithErrors(HttpStatus.BAD_REQUEST, "Validation failed", errors, req);
    }

    // ---- 400 BAD REQUEST : custom domain validation failure (with field errors) ----
    @ExceptionHandler(ValidationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleValidationFailed(ValidationFailedException ex, HttpServletRequest req) {
        List<String> errors = new ArrayList<>();
        ex.getFieldErrors().forEach((field, message) -> errors.add(field + ": " + message));
        String message = ex.getMessage() != null ? ex.getMessage() : "Validation failed";
        return buildWithErrors(HttpStatus.BAD_REQUEST, message, errors, req);
    }

    // ---- 400 BAD REQUEST : Bean Validation on path/query params (@Validated) ----
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        List<String> errors = new ArrayList<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            errors.add(v.getPropertyPath() + ": " + v.getMessage());
        }
        return buildWithErrors(HttpStatus.BAD_REQUEST, "Validation failed", errors, req);
    }

    // ---- 400 BAD REQUEST : other client errors ----
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(InvalidRequestException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBalance(InsufficientBalanceException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Malformed or missing request body", req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Missing required parameter: " + ex.getParameterName(), req);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Missing required header: " + ex.getHeaderName(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter: " + ex.getName(), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    // ---- 401 UNAUTHORIZED : failed authentication (e.g. bad login credentials) ----
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Authentication failed: " + ex.getMessage(), req);
    }

    // ---- 403 FORBIDDEN : method-level authorization (@PreAuthorize) ----
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Access is denied", req);
    }

    // ---- 405 METHOD NOT ALLOWED ----
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), req);
    }

    // ---- 415 UNSUPPORTED MEDIA TYPE ----
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), req);
    }

    // ---- 409 CONFLICT : duplicate resource / data integrity ----
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Data integrity violation (possible duplicate or constraint failure)", req);
    }

    // ---- 500 INTERNAL SERVER ERROR : last-resort fallback ----
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), req);
    }

    // ---- helpers ----
    private Map<String, Object> baseBody(HttpStatus status, String message, HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.name());          // e.g. "BAD_REQUEST" (kept for existing tests)
        body.put("statusCode", status.value());      // e.g. 400
        body.put("error", status.getReasonPhrase()); // e.g. "Bad Request"
        body.put("message", message);
        if (req != null) {
            body.put("path", req.getRequestURI());
        }
        return body;
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, HttpServletRequest req) {
        return new ResponseEntity<>(baseBody(status, message, req), status);
    }

    private ResponseEntity<Map<String, Object>> buildWithErrors(HttpStatus status, String message,
                                                                List<String> errors, HttpServletRequest req) {
        Map<String, Object> body = baseBody(status, message, req);
        body.put("errors", errors);
        return new ResponseEntity<>(body, status);
    }
}
