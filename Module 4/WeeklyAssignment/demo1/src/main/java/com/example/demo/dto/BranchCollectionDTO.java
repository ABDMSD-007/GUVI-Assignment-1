package com.example.demo.dto;

/**
 * Branch-wise total EMI collection (Task 4.2 + dashboard / top branches).
 */
public record BranchCollectionDTO(
        String branchName,
        Double totalCollected
) {
}
