package com.example.demo.repository;

import com.example.demo.dto.BranchCollectionDTO;
import com.example.demo.entity.Loan;
import com.example.demo.enums.LoanStatus;
import com.example.demo.enums.LoanType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    // ---- Task 3: Derived queries ----
    List<Loan> findByLoanType(LoanType loanType);
    List<Loan> findByLoanStatus(LoanStatus status);
    List<Loan> findByInterestRateGreaterThan(double rate);

    // ---- Task 4.2: Branch-wise total EMI collection ----
    @Query("SELECT new com.example.demo.dto.BranchCollectionDTO(c.branchName, COALESCE(SUM(e.amountPaid), 0)) " +
           "FROM Loan l JOIN l.customer c JOIN l.emiTransactions e " +
           "GROUP BY c.branchName ORDER BY SUM(e.amountPaid) DESC")
    List<BranchCollectionDTO> getBranchWiseEMICollection();

    // ---- Task 4.5: Loans with no penalty history ----
    @Query("SELECT l FROM Loan l LEFT JOIN l.penalties p WHERE p IS NULL")
    List<Loan> findLoansWithNoPenalty();

    // ---- Task 4.6: Top 5 customers paying highest EMI ----
    @Query("SELECT l.customer.customerName, SUM(e.amountPaid) AS total " +
           "FROM Loan l JOIN l.emiTransactions e " +
           "GROUP BY l.customer.customerName ORDER BY total DESC")
    List<Object[]> findTop5CustomersByEMI(Pageable pageable);

    // ---- Task 5: Update query - increase interest rate of Personal loans by 0.5% ----
    @Modifying
    @Transactional
    @Query("UPDATE Loan l SET l.interestRate = l.interestRate + 0.5 WHERE l.loanType = com.example.demo.enums.LoanType.PERSONAL")
    int increaseInterestRate();

    // ---- Task 6: Pagination + sorting (active loans only, soft delete aware) ----
    @Query("SELECT l FROM Loan l WHERE l.active = true")
    Page<Loan> findAllActive(Pageable pageable);

    // ---- Final Challenge: dashboard aggregates ----
    long countByLoanStatus(LoanStatus status);

    @Query("SELECT COALESCE(MAX(l.principalAmount), 0) FROM Loan l")
    Double findHighestLoanAmount();

    // ---- Bonus: Overdue EMIs older than 30 days ----
    @Query("SELECT DISTINCT l FROM Loan l JOIN l.emiTransactions e " +
           "WHERE e.paymentStatus = com.example.demo.enums.PaymentStatus.PENDING AND e.paymentDate < :cutoff")
    List<Loan> findOverdueLoans(@Param("cutoff") java.time.LocalDate cutoff);
}

