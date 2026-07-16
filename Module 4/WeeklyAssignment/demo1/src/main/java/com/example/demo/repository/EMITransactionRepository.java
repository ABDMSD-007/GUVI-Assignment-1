package com.example.demo.repository;

import com.example.demo.dto.MonthlyCollectionDTO;
import com.example.demo.entity.EMITransaction;
import com.example.demo.enums.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EMITransactionRepository extends JpaRepository<EMITransaction, Long> {

    // ---- Task 3: Derived query ----
    List<EMITransaction> findByPaymentStatus(PaymentStatus status);

    // ---- Task 4.4: Latest EMI payment (LIMIT 1 via Pageable) ----
    @Query("SELECT e FROM EMITransaction e ORDER BY e.paymentDate DESC")
    List<EMITransaction> findLatestPayments(Pageable pageable);

    // EMIs for a given loan
    List<EMITransaction> findByLoan_LoanId(Long loanId);

    // ---- Dashboard: total EMI collected ----
    @Query("SELECT COALESCE(SUM(e.amountPaid), 0) FROM EMITransaction e WHERE e.paymentStatus = com.example.demo.enums.PaymentStatus.PAID")
    Double getTotalEMICollected();

    long countByLoan_LoanIdAndPaymentStatus(Long loanId, PaymentStatus paymentStatus);

    // ---- Bonus: Monthly collection report ----
    @Query("SELECT new com.example.demo.dto.MonthlyCollectionDTO(" +
           "YEAR(e.paymentDate), MONTH(e.paymentDate), COALESCE(SUM(e.amountPaid), 0)) " +
           "FROM EMITransaction e GROUP BY YEAR(e.paymentDate), MONTH(e.paymentDate) " +
           "ORDER BY YEAR(e.paymentDate), MONTH(e.paymentDate)")
    List<MonthlyCollectionDTO> getMonthlyCollectionReport();
}

