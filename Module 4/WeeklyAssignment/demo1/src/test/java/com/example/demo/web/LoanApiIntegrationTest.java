package com.example.demo.web;

import com.example.demo.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests for the Loan API, covering the global exception
 * handling (404 not found, 400 invalid enum path variable) and pagination JSON shape.
 */
class LoanApiIntegrationTest extends AbstractWebIntegrationTest {

    @Test
    void getLoans_returnsPagedJson() throws Exception {
        String token = registerAndLogin(uniqueEmail(), Role.USER);
        mockMvc.perform(get("/loans").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getLoanById_notFound_returns404WithErrorBody() throws Exception {
        String token = registerAndLogin(uniqueEmail(), Role.USER);
        mockMvc.perform(get("/loans/999999").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/loans/999999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void loansByInvalidType_returns400() throws Exception {
        String token = registerAndLogin(uniqueEmail(), Role.USER);
        mockMvc.perform(get("/loans/type/NOT_A_TYPE").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void loansByValidType_returnsOk() throws Exception {
        String token = registerAndLogin(uniqueEmail(), Role.USER);
        mockMvc.perform(get("/loans/type/PERSONAL").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

