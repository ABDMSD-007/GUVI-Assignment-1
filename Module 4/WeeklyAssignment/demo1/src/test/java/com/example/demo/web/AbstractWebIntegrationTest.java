package com.example.demo.web;

import com.example.demo.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base for full-stack (controller + security + JPA/H2) integration tests.
 * Provides helpers to register a customer and obtain a real JWT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractWebIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@nbfc.com";
    }

    protected void register(String email, Role role) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("customerName", "Test User");
        body.put("email", email);
        body.put("password", "secret");
        body.put("mobileNumber", "9999999999");
        body.put("branchName", "Bangalore");
        body.put("creditScore", 750);
        if (role != null) {
            body.put("role", role.name());
        }
        mockMvc.perform(post("/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    protected String login(String email, String password) throws Exception {
        Map<String, String> body = Map.of("username", email, "password", password);
        return mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    protected String registerAndLogin(String email, Role role) throws Exception {
        register(email, role);
        return login(email, "secret");
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}

