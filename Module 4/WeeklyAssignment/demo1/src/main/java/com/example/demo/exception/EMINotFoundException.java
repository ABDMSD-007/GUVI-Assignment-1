package com.example.demo.exception;

public class EMINotFoundException extends RuntimeException {
    public EMINotFoundException(String message) {
        super(message);
    }
}

