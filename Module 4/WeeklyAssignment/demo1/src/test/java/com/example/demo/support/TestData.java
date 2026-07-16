package com.example.demo.support;

import com.example.demo.entity.Customer;
import com.example.demo.entity.EMITransaction;
import com.example.demo.entity.Loan;
import com.example.demo.entity.Penalty;
import com.example.demo.enums.LoanStatus;
import com.example.demo.enums.LoanType;
import com.example.demo.enums.PaymentMode;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.enums.Role;

import java.time.LocalDate;

/**
 * Factory helpers to build valid (bean-validation compliant) entities for tests.
 */
public final class TestData {

    private TestData() {
    }

    public static Customer customer(String name, String email, String branch, int creditScore, Role role) {
        Customer c = new Customer();
        c.setCustomerName(name);
        c.setEmail(email);
        c.setPassword("secret");
        c.setMobileNumber("9999999999");
        c.setBranchName(branch);
        c.setCreditScore(creditScore);
        c.setRole(role);
        return c;
    }

    public static Loan loan(Customer owner, LoanType type, double principal, double rate,
                            int tenure, double emi, LoanStatus status, boolean active) {
        Loan l = new Loan();
        l.setCustomer(owner);
        l.setLoanType(type);
        l.setPrincipalAmount(principal);
        l.setInterestRate(rate);
        l.setTenureMonths(tenure);
        l.setEmiAmount(emi);
        l.setLoanStatus(status);
        l.setActive(active);
        return l;
    }

    public static EMITransaction emi(Loan loan, int installment, double amountPaid,
                                     LocalDate date, PaymentMode mode, PaymentStatus status) {
        EMITransaction e = new EMITransaction();
        e.setLoan(loan);
        e.setInstallmentNumber(installment);
        e.setAmountPaid(amountPaid);
        e.setPaymentDate(date);
        e.setPaymentMode(mode);
        e.setPaymentStatus(status);
        return e;
    }

    public static Penalty penalty(Loan loan, double amount, String reason, LocalDate date) {
        Penalty p = new Penalty();
        p.setLoan(loan);
        p.setPenaltyAmount(amount);
        p.setReason(reason);
        p.setPenaltyDate(date);
        return p;
    }
}

