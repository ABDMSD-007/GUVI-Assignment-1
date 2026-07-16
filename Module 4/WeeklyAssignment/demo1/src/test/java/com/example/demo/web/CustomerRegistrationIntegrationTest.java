package com.example.demo.web;

import com.example.demo.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for customer registration, including the new
 * {@code DuplicateEmailException} (409) and bean-validation (400) handling.
 */
class CustomerRegistrationIntegrationTest extends AbstractWebIntegrationTest {

    @Test
    void register_returnsCreatedAndDefaultsRoleToUser() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerName", "Rahul");
        body.put("email", uniqueEmail());
        body.put("password", "secret");
        body.put("mobileNumber", "9999999999");
        body.put("branchName", "Bangalore");
        body.put("creditScore", 750);

        mockMvc.perform(post("/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").exists())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_duplicateEmail_returnsConflict() throws Exception {
        String email = uniqueEmail();
        register(email, Role.USER);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerName", "Someone Else");
        body.put("email", email);
        body.put("password", "secret");
        body.put("mobileNumber", "8888888888");
        body.put("branchName", "Chennai");
        body.put("creditScore", 700);

        mockMvc.perform(post("/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(email)))
                .andExpect(jsonPath("$.path").value("/customers/register"));
    }

    @Test
    void register_invalidEmail_returnsBadRequest() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerName", "Bad Email");
        body.put("email", "not-an-email");
        body.put("password", "secret");
        body.put("mobileNumber", "9999999999");
        body.put("branchName", "Bangalore");
        body.put("creditScore", 750);

        mockMvc.perform(post("/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void register_blankName_returnsBadRequest() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerName", "");
        body.put("email", uniqueEmail());
        body.put("password", "secret");
        body.put("mobileNumber", "9999999999");
        body.put("branchName", "Bangalore");
        body.put("creditScore", 750);

        mockMvc.perform(post("/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}

