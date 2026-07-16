package com.example.demo.dto;

/**
 * Task 7 - DTO Projection (JPQL Constructor Expression).
 */
public record CustomerSummaryDTO(
        String customerName,
        String branchName,
        Long numberOfLoans,
        Double totalEMIPaid,
        Double totalPenaltyPaid
) {
}