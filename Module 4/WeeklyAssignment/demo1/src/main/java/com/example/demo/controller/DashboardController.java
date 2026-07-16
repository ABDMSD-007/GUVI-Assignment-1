package com.example.demo.controller;

import com.example.demo.dto.DashboardDTO;
import com.example.demo.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Final Challenge - GET /dashboard.
 * Computed with a minimum number of optimized aggregate JPQL queries (no N+1).
 */
@RestController
@Slf4j
@Tag(name = "Dashboard", description = "Aggregated analytics for managers")
public class DashboardController {

    private final LoanService loanService;

    public DashboardController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Dashboard analytics", description = "Aggregated NBFC stats. Requires MANAGER role.")
    public ResponseEntity<DashboardDTO> dashboard() {
        log.info("GET /dashboard - building analytics dashboard");
        return ResponseEntity.ok(loanService.getDashboard());
    }
}

