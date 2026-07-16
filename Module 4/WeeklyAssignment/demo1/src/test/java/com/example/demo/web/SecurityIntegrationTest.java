package com.example.demo.web;

import com.example.demo.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack security integration tests: JWT login, unauthorized access,
 * and role-based authorization (USER vs MANAGER) enforced by @PreAuthorize.
 */
class SecurityIntegrationTest extends AbstractWebIntegrationTest {

    @Test
    void login_returnsValidJwt() throws Exception {
        String email = uniqueEmail();
        register(email, Role.USER);
        String token = login(email, "secret");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() throws Exception {
        String email = uniqueEmail();
        register(email, Role.USER);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", email, "password", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_isRejected() throws Exception {
        mockMvc.perform(get("/loans"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void userRole_canAccessLoans() throws Exception {
        String token = registerAndLogin(uniqueEmail(), Role.USER);
        mockMvc.perform(get("/loans").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void userRole_cannotAccessDashboard_isForbidden() throws Exception {
        String token = registerAndLogin(uniqueEmail(), Role.USER);
        mockMvc.perform(get("/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerRole_canAccessDashboard() throws Exception {
        String token = registerAndLogin(uniqueEmail(), Role.MANAGER);
        mockMvc.perform(get("/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void adminRole_inheritsUserAccess_viaRoleHierarchy() throws Exception {
        String token = registerAndLogin(uniqueEmail(), Role.ADMIN);
        // ADMIN > MANAGER > USER, so ADMIN can call a USER-only endpoint
        mockMvc.perform(get("/loans").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }
}

