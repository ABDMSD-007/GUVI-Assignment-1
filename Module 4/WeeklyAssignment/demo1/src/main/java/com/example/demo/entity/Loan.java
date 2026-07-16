package com.example.demo.entity;

import com.example.demo.enums.LoanStatus;
import com.example.demo.enums.LoanType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loanId;

    @Enumerated(EnumType.STRING)
    private LoanType loanType; // PERSONAL, HOME, VEHICLE, EDUCATION

    @Positive
    private Double principalAmount;

    @Positive
    private Double interestRate;

    @Positive
    private Integer tenureMonths;

    @PositiveOrZero
    private Double emiAmount;

    @Enumerated(EnumType.STRING)
    private LoanStatus loanStatus; // ACTIVE, CLOSED, DEFAULTED

    // Soft delete flag (Bonus): inactive loans must be ignored by queries
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Customer customer;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<EMITransaction> emiTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<Penalty> penalties = new ArrayList<>();
}

