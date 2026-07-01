package org.northernarc.assessment4.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Task 8: Login payload. Validated with @Valid in the controller so blank/invalid
 * credentials are rejected with 400 (and clear messages) before authentication.
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email should be a valid address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}

