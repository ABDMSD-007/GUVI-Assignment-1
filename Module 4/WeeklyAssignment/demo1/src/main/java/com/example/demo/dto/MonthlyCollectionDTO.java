package com.example.demo.dto;

/**
 * Monthly EMI + penalty collection report (Bonus Challenge).
 */
public record MonthlyCollectionDTO(
        Integer year,
        Integer month,
        Double totalCollected
) {
}
