package com.example.demo.entity;

import com.example.demo.enums.PaymentMode;
import com.example.demo.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "emi_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EMITransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @NotNull
    @Positive
    private Integer installmentNumber;

    @Positive
    private Double amountPaid;

    @NotNull
    private LocalDate paymentDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode; // UPI, CARD, NETBANKING, CASH

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus; // PAID, MISSED, PENDING

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Loan loan;
}

