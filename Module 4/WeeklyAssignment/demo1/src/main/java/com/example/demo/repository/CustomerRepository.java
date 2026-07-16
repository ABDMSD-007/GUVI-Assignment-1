package com.example.demo.repository;

import com.example.demo.dto.CustomerSummaryDTO;
import com.example.demo.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // ---- Task 3: Derived queries ----
    List<Customer> findByBranchName(String branchName);
    List<Customer> findByCreditScoreGreaterThan(int score);

    // Used by JWT UserDetailsService
    Optional<Customer> findByEmail(String email);

    // ---- Task 4.1: Customers with more than N loans ----
    @Query("SELECT c FROM Customer c JOIN c.loans l GROUP BY c HAVING COUNT(l) > :minimumLoans")
    List<Customer> findCustomersWithMoreThanNLoans(@Param("minimumLoans") long minimumLoans);

    // ---- Task 4.3: Customers having multiple distinct loan types ----
    @Query("SELECT c FROM Customer c JOIN c.loans l GROUP BY c HAVING COUNT(DISTINCT l.loanType) > 1")
    List<Customer> findCustomersWithMultipleLoanTypes();

    // ---- Task 7: DTO Projection (Constructor Expression) ----
    @Query("SELECT new com.example.demo.dto.CustomerSummaryDTO(" +
           "c.customerName, c.branchName, COUNT(DISTINCT l), " +
           "COALESCE(SUM(e.amountPaid), 0), COALESCE(SUM(p.penaltyAmount), 0)) " +
           "FROM Customer c " +
           "LEFT JOIN c.loans l " +
           "LEFT JOIN l.emiTransactions e " +
           "LEFT JOIN l.penalties p " +
           "GROUP BY c.customerId, c.customerName, c.branchName")
    List<CustomerSummaryDTO> getCustomerSummaries();

    // ---- Bonus: eligibility threshold ----
    @Query("SELECT c FROM Customer c WHERE c.creditScore > :threshold")
    List<Customer> findEligibleCustomers(@Param("threshold") int threshold);
}

