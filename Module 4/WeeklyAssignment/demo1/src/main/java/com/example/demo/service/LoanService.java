package com.example.demo.service;

import com.example.demo.dto.BranchCollectionDTO;
import com.example.demo.dto.CustomerSummaryDTO;
import com.example.demo.dto.DashboardDTO;
import com.example.demo.dto.MonthlyCollectionDTO;
import com.example.demo.entity.Customer;
import com.example.demo.entity.EMITransaction;
import com.example.demo.entity.Loan;
import com.example.demo.enums.LoanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LoanService {

    // Task 6 - paginated, sorted by emiAmount DESC
    Page<Loan> getAllLoans(Pageable pageable);

    Loan getLoanById(Long id);
    void deleteLoan(Long id);                 // ADMIN, soft delete
    Loan approveLoan(Long id);                // MANAGER
    int increaseInterestRate();               // MANAGER, Task 5
    EMITransaction payEmi(Long loanId, EMITransaction emi);  // USER
    List<EMITransaction> getEmiSchedule(Long loanId);        // USER

    // Task 3 derived
    List<Loan> getLoansByType(LoanType type);
    List<Customer> getCustomersByBranch(String branch);

    // Task 4 JPQL
    List<Customer> getCustomersWithMoreThanNLoans(long minimumLoans);
    List<BranchCollectionDTO> getBranchWiseCollection();
    List<Customer> getCustomersWithMultipleLoanTypes();
    EMITransaction getLatestPayment();
    List<Loan> getLoansWithoutPenalty();
    List<Object[]> getTop5CustomersByEMI();

    // Task 7
    List<CustomerSummaryDTO> getCustomerSummaries();

    // Final challenge
    DashboardDTO getDashboard();

    // Bonus
    List<Loan> getOverdueLoans();
    Loan forecloseLoan(Long id);
    List<BranchCollectionDTO> getTop10Branches();
    List<Customer> getEligibleCustomers();
    List<MonthlyCollectionDTO> getMonthlyReport();
}

