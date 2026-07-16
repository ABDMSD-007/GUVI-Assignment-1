package com.example.demo.controller;

import com.example.demo.entity.Customer;
import com.example.demo.enums.Role;
import com.example.demo.exception.DuplicateEmailException;
import com.example.demo.repository.CustomerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
@Slf4j
@Tag(name = "Customers", description = "Customer registration")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerController(CustomerRepository customerRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // permitAll - lets you create a login-capable account
    @PostMapping("/register")
    @SecurityRequirements // public endpoint - no JWT required
    @Operation(summary = "Register customer", description = "Creates a login-capable account (password is BCrypt-encoded).")
    public ResponseEntity<Customer> register(@Valid @RequestBody Customer customer) {
        log.info("POST /customers/register - registering customer email='{}'", customer.getEmail());
        if (customerRepository.findByEmail(customer.getEmail()).isPresent()) {
            log.warn("POST /customers/register - duplicate email '{}'", customer.getEmail());
            throw new DuplicateEmailException("Email already registered: " + customer.getEmail());
        }
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        if (customer.getRole() == null) customer.setRole(Role.USER);
        Customer saved = customerRepository.save(customer);
        log.info("POST /customers/register - customer registered id={}", saved.getCustomerId());
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }
}