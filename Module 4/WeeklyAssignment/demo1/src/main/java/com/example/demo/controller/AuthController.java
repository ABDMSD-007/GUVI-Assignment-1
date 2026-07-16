package com.example.demo.controller;

import com.example.demo.dto.AuthRequest;
import com.example.demo.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@Tag(name = "Authentication", description = "Login and JWT issuance")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthController(JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    // Task 8 - POST /login permitAll
    @PostMapping("/login")
    @SecurityRequirements // public endpoint - no JWT required
    @Operation(summary = "Login", description = "Authenticates a customer and returns a signed JWT.")
    public String login(@RequestBody AuthRequest authRequest) {
        log.info("POST /login - authentication attempt for user '{}'", authRequest.username());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.username(), authRequest.password()));
        String token = jwtUtil.generateToken(authRequest.username());
        log.info("POST /login - authentication succeeded for user '{}'", authRequest.username());
        return token;
    }
}

