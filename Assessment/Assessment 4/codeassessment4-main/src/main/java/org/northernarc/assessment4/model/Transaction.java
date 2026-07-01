package org.northernarc.assessment4.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    // Task 1: Primary key mapping
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    // Task 2: Bean Validation
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @NotBlank(message = "Transaction type is required")
    private String transactionType;

    // Task 1: Transaction * -------- 1 Account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_number")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Account account;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;
}
