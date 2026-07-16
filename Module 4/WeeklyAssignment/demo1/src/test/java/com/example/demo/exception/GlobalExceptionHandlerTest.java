package com.example.demo.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest reqAt(String path) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getRequestURI()).thenReturn(path);
        return req;
    }

    @Test
    void customerNotFound_returns404WithFields() {
        ResponseEntity<ErrorResponse> resp = handler.handleCustomerNotFound(
                new CustomerNotFoundException("missing"), reqAt("/customers/9"));
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        ErrorResponse body = resp.getBody();
        assertNotNull(body);
        assertEquals(404, body.status());
        assertEquals("missing", body.message());
        assertEquals("/customers/9", body.path());
        assertNotNull(body.timestamp());
    }

    @Test
    void loanNotFound_returns404() {
        ResponseEntity<ErrorResponse> resp = handler.handleLoanNotFound(
                new LoanNotFoundException("no loan"), reqAt("/loans/1"));
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void emiNotFound_returns404() {
        ResponseEntity<ErrorResponse> resp = handler.handleEMINotFound(
                new EMINotFoundException("no emi"), reqAt("/emi/1"));
        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void illegalArgument_returns400() {
        ResponseEntity<ErrorResponse> resp = handler.handleIllegalArgument(
                new IllegalArgumentException("bad"), reqAt("/loans/1/foreclose"));
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }
}
