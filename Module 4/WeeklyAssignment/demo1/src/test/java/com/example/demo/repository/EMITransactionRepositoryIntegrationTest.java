package com.example.demo.repository;

import com.example.demo.dto.MonthlyCollectionDTO;
import com.example.demo.entity.Customer;
import com.example.demo.entity.EMITransaction;
import com.example.demo.entity.Loan;
import com.example.demo.enums.LoanStatus;
import com.example.demo.enums.LoanType;
import com.example.demo.enums.PaymentMode;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.support.TestData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link EMITransactionRepository} and {@link PenaltyRepository} against H2.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EMITransactionRepositoryIntegrationTest {

    @Autowired
    private EMITransactionRepository emiRepository;

    @Autowired
    private PenaltyRepository penaltyRepository;

    @PersistenceContext
    private EntityManager em;

    private Long loanId;

    @BeforeEach
    void seed() {
        Customer customer = TestData.customer("Rahul", "rahul@nbfc.com", "Bangalore", 780, null);
        em.persist(customer);
        Loan loan = TestData.loan(customer, LoanType.PERSONAL, 100000, 10.0, 12, 9000, LoanStatus.ACTIVE, true);
        em.persist(loan);
        loanId = loan.getLoanId();

        em.persist(TestData.emi(loan, 1, 9000, LocalDate.of(2026, 1, 15), PaymentMode.UPI, PaymentStatus.PAID));
        em.persist(TestData.emi(loan, 2, 9000, LocalDate.of(2026, 2, 15), PaymentMode.CARD, PaymentStatus.PAID));
        em.persist(TestData.emi(loan, 3, 9000, LocalDate.of(2026, 3, 15), PaymentMode.CASH, PaymentStatus.PENDING));

        em.persist(TestData.penalty(loan, 300, "late", LocalDate.of(2026, 3, 20)));
        em.persist(TestData.penalty(loan, 200, "late", LocalDate.of(2026, 4, 20)));

        em.flush();
        em.clear();
    }

    @Test
    void getTotalEMICollected_sumsOnlyPaid() {
        assertThat(emiRepository.getTotalEMICollected()).isEqualTo(18000.0);
    }

    @Test
    void findByPaymentStatus_filtersByStatus() {
        assertThat(emiRepository.findByPaymentStatus(PaymentStatus.PAID)).hasSize(2);
        assertThat(emiRepository.findByPaymentStatus(PaymentStatus.PENDING)).hasSize(1);
    }

    @Test
    void countByLoanAndStatus() {
        assertThat(emiRepository.countByLoan_LoanIdAndPaymentStatus(loanId, PaymentStatus.PAID)).isEqualTo(2);
        assertThat(emiRepository.countByLoan_LoanIdAndPaymentStatus(loanId, PaymentStatus.PENDING)).isEqualTo(1);
    }

    @Test
    void findLatestPayments_orderedByDateDesc() {
        List<EMITransaction> latest = emiRepository.findLatestPayments(PageRequest.of(0, 1));
        assertThat(latest).hasSize(1);
        assertThat(latest.get(0).getPaymentDate()).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void getMonthlyCollectionReport_groupsByYearMonth() {
        List<MonthlyCollectionDTO> report = emiRepository.getMonthlyCollectionReport();
        // Jan, Feb, Mar => 3 buckets
        assertThat(report).hasSize(3);
        assertThat(report.get(0).year()).isEqualTo(2026);
        assertThat(report.get(0).month()).isEqualTo(1);
        assertThat(report.get(0).totalCollected()).isEqualTo(9000.0);
    }

    @Test
    void penaltyRepository_aggregatesAndCounts() {
        assertThat(penaltyRepository.getTotalPenaltyCollected()).isEqualTo(500.0);
        assertThat(penaltyRepository.countByLoan_LoanId(loanId)).isEqualTo(2);
        assertThat(penaltyRepository.findByLoan_LoanId(loanId)).hasSize(2);
    }
}



