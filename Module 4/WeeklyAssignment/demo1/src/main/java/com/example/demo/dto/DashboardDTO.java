package com.example.demo.dto;

import lombok.Builder;

/**
 * Final Challenge - /dashboard analytics response.
 */
@Builder
public record DashboardDTO(
        Long totalCustomers,
        Long activeLoans,
        Long closedLoans,
        Double totalEMICollected,
        Double totalPenaltyCollected,
        String topBranch,
        String highestPayingCustomer,
        Double highestLoanAmount,
        Long defaultedLoans
) {
}
