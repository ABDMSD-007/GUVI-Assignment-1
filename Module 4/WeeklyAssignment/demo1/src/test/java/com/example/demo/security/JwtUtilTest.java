package com.example.demo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final UserDetails user = User.withUsername("rahul@nbfc.com")
            .password("x").roles("USER").build();

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "your-super-secret-key-that-is-at-least-32-characters-long!");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
    }

    @Test
    void generateAndExtractUsername() {
        String token = jwtUtil.generateToken("rahul@nbfc.com");
        assertEquals("rahul@nbfc.com", jwtUtil.extractUsername(token));
    }

    @Test
    void validToken_passes() {
        String token = jwtUtil.generateToken("rahul@nbfc.com");
        assertTrue(jwtUtil.validateToken(token, user));
    }

    @Test
    void validateToken_wrongUser_fails() {
        String token = jwtUtil.generateToken("someone-else@nbfc.com");
        assertFalse(jwtUtil.validateToken(token, user));
    }

    @Test
    void expiredToken_fails() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L); // already expired
        String token = jwtUtil.generateToken("rahul@nbfc.com");
        // JJWT rejects expired tokens by throwing during parsing
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> jwtUtil.validateToken(token, user));
    }

    @Test
    void tamperedToken_throws() {
        String token = jwtUtil.generateToken("rahul@nbfc.com") + "abc";
        assertThrows(Exception.class, () -> jwtUtil.extractUsername(token));
    }
}


