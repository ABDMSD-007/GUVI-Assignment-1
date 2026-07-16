package com.example.demo.repository;

import com.example.demo.dto.CustomerSummaryDTO;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link CustomerRepository} against H2:
 * derived lookups, HAVING-based JPQL and the constructor-expression projection.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerRepositoryIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @PersistenceContext
    private EntityManager em;

    @BeforeEach
    void seed() {
        // Customer with 2 loans of 2 distinct types, eligible (creditScore > 700)
        Customer rahul = TestData.customer("Rahul", "rahul@nbfc.com", "Bangalore", 780, null);
        em.persist(rahul);
        Loan personal = TestData.loan(rahul, LoanType.PERSONAL, 100000, 10.0, 12, 9000, LoanStatus.ACTIVE, true);
        em.persist(personal);
        em.persist(TestData.loan(rahul, LoanType.HOME, 5000000, 8.5, 240, 40000, LoanStatus.ACTIVE, true));
        em.persist(TestData.emi(personal, 1, 9000, LocalDate.now(), PaymentMode.UPI, PaymentStatus.PAID));
        em.persist(TestData.penalty(personal, 250, "late", LocalDate.now()));

        // Customer with 1 loan, not eligible (creditScore < 700)
        Customer priya = TestData.customer("Priya", "priya@nbfc.com", "Chennai", 650, null);
        em.persist(priya);
        em.persist(TestData.loan(priya, LoanType.VEHICLE, 800000, 9.0, 60, 15000, LoanStatus.ACTIVE, true));

        em.flush();
        em.clear();
    }

    @Test
    void findByEmail_findsExisting() {
        Optional<Customer> found = customerRepository.findByEmail("rahul@nbfc.com");
        assertThat(found).isPresent();
        assertThat(found.get().getBranchName()).isEqualTo("Bangalore");
    }

    @Test
    void findByEmail_missingReturnsEmpty() {
        assertThat(customerRepository.findByEmail("none@x.com")).isEmpty();
    }

    @Test
    void findCustomersWithMoreThanNLoans() {
        List<Customer> moreThanOne = customerRepository.findCustomersWithMoreThanNLoans(1);
        assertThat(moreThanOne).extracting(Customer::getCustomerName).containsExactly("Rahul");
    }

    @Test
    void findCustomersWithMultipleLoanTypes() {
        List<Customer> multi = customerRepository.findCustomersWithMultipleLoanTypes();
        assertThat(multi).extracting(Customer::getCustomerName).containsExactly("Rahul");
    }

    @Test
    void findEligibleCustomers_usesThreshold() {
        List<Customer> eligible = customerRepository.findEligibleCustomers(700);
        assertThat(eligible).extracting(Customer::getCustomerName).containsExactly("Rahul");
    }

    @Test
    void getCustomerSummaries_projectsAggregates() {
        List<CustomerSummaryDTO> summaries = customerRepository.getCustomerSummaries();
        assertThat(summaries).hasSize(2);

        CustomerSummaryDTO rahul = summaries.stream()
                .filter(s -> s.customerName().equals("Rahul")).findFirst().orElseThrow();
        assertThat(rahul.numberOfLoans()).isEqualTo(2);
        assertThat(rahul.totalEMIPaid()).isEqualTo(9000.0);
        assertThat(rahul.totalPenaltyPaid()).isEqualTo(250.0);

        CustomerSummaryDTO priya = summaries.stream()
                .filter(s -> s.customerName().equals("Priya")).findFirst().orElseThrow();
        assertThat(priya.numberOfLoans()).isEqualTo(1);
        assertThat(priya.totalEMIPaid()).isEqualTo(0.0);
    }
}


