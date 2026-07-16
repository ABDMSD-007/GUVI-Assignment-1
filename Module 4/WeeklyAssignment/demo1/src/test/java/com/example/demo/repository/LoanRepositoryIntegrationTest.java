package com.example.demo.repository;

import com.example.demo.dto.BranchCollectionDTO;
import com.example.demo.entity.Customer;
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
 * Integration tests for {@link LoanRepository} against an in-memory H2 database.
 * Exercises derived queries, JPQL aggregates, the soft-delete filter and the bulk update.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoanRepositoryIntegrationTest {

    @Autowired
    private LoanRepository loanRepository;

    @PersistenceContext
    private EntityManager em;

    @BeforeEach
    void seed() {
        Customer bangaloreCustomer = TestData.customer("Rahul", "rahul@nbfc.com", "Bangalore", 780, null);
        Customer chennaiCustomer = TestData.customer("Priya", "priya@nbfc.com", "Chennai", 720, null);
        em.persist(bangaloreCustomer);
        em.persist(chennaiCustomer);

        // Personal, active, has EMI payments, no penalty
        Loan personal = TestData.loan(bangaloreCustomer, LoanType.PERSONAL, 100000, 10.0, 12, 9000, LoanStatus.ACTIVE, true);
        em.persist(personal);
        em.persist(TestData.emi(personal, 1, 9000, LocalDate.now().minusDays(10), PaymentMode.UPI, PaymentStatus.PAID));
        em.persist(TestData.emi(personal, 2, 9000, LocalDate.now().minusDays(5), PaymentMode.CARD, PaymentStatus.PAID));

        // Home, closed, has a penalty and an overdue pending EMI
        Loan home = TestData.loan(chennaiCustomer, LoanType.HOME, 5000000, 8.5, 240, 40000, LoanStatus.CLOSED, true);
        em.persist(home);
        em.persist(TestData.emi(home, 1, 40000, LocalDate.now().minusDays(60), PaymentMode.NETBANKING, PaymentStatus.PENDING));
        em.persist(TestData.penalty(home, 500, "late", LocalDate.now().minusDays(40)));

        // Soft-deleted loan (should be hidden by findAllActive)
        em.persist(TestData.loan(bangaloreCustomer, LoanType.VEHICLE, 800000, 9.0, 60, 15000, LoanStatus.DEFAULTED, false));

        em.flush();
        em.clear();
    }

    @Test
    void findByLoanType_returnsOnlyMatching() {
        List<Loan> personal = loanRepository.findByLoanType(LoanType.PERSONAL);
        assertThat(personal).hasSize(1);
        assertThat(personal.get(0).getLoanType()).isEqualTo(LoanType.PERSONAL);
    }

    @Test
    void countByLoanStatus_counts() {
        assertThat(loanRepository.countByLoanStatus(LoanStatus.ACTIVE)).isEqualTo(1);
        assertThat(loanRepository.countByLoanStatus(LoanStatus.CLOSED)).isEqualTo(1);
        assertThat(loanRepository.countByLoanStatus(LoanStatus.DEFAULTED)).isEqualTo(1);
    }

    @Test
    void findAllActive_excludesSoftDeleted() {
        var page = loanRepository.findAllActive(PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2); // vehicle loan is inactive
        assertThat(page.getContent()).allMatch(Loan::getActive);
    }

    @Test
    void findHighestLoanAmount_returnsMaxPrincipal() {
        assertThat(loanRepository.findHighestLoanAmount()).isEqualTo(5000000.0);
    }

    @Test
    void getBranchWiseEMICollection_sumsPerBranch() {
        List<BranchCollectionDTO> result = loanRepository.getBranchWiseEMICollection();
        assertThat(result).isNotEmpty();
        BranchCollectionDTO bangalore = result.stream()
                .filter(b -> b.branchName().equals("Bangalore")).findFirst().orElseThrow();
        assertThat(bangalore.totalCollected()).isEqualTo(18000.0); // 9000 + 9000
    }

    @Test
    void findLoansWithNoPenalty_returnsLoansWithoutPenalties() {
        List<Loan> noPenalty = loanRepository.findLoansWithNoPenalty();
        // personal + vehicle have no penalty; home does
        assertThat(noPenalty).extracting(Loan::getLoanType)
                .contains(LoanType.PERSONAL, LoanType.VEHICLE)
                .doesNotContain(LoanType.HOME);
    }

    @Test
    void increaseInterestRate_updatesOnlyPersonalLoans() {
        int updated = loanRepository.increaseInterestRate();
        em.flush();
        em.clear();

        assertThat(updated).isEqualTo(1);
        Loan personal = loanRepository.findByLoanType(LoanType.PERSONAL).get(0);
        assertThat(personal.getInterestRate()).isEqualTo(10.5); // 10.0 + 0.5
        Loan home = loanRepository.findByLoanType(LoanType.HOME).get(0);
        assertThat(home.getInterestRate()).isEqualTo(8.5); // unchanged
    }

    @Test
    void findOverdueLoans_returnsPendingOlderThanCutoff() {
        List<Loan> overdue = loanRepository.findOverdueLoans(LocalDate.now().minusDays(30));
        assertThat(overdue).hasSize(1);
        assertThat(overdue.get(0).getLoanType()).isEqualTo(LoanType.HOME);
    }
}


