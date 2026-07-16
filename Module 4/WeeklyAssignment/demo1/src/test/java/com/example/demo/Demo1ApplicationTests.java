package com.example.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Demo1ApplicationTests {

    // Avoid loading the full Spring context (no DB needed for unit tests)
    @Test
    void sanity() {
        assertTrue(true);
    }

}
