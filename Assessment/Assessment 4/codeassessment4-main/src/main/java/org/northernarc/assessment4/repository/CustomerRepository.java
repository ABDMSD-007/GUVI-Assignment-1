package org.northernarc.assessment4.repository;

import org.northernarc.assessment4.dto.BranchBalance;
import org.northernarc.assessment4.dto.CustomerSummaryDTO;
import org.northernarc.assessment4.model.Customer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Task 3: Derived Query Method
    List<Customer> findByBranch(String branch);


    // Security Helper
    java.util.Optional<Customer> findByEmail(String email);

    // Task 4: Find Rich Customers (customers owning an account whose balance exceeds a threshold)
    @Query("SELECT DISTINCT c FROM Customer c JOIN c.accounts a WHERE a.balance > :threshold")
    List<Customer> findRichCustomers(@Param("threshold") double threshold);

    // Task 4: Find Total Balance Per Branch (GROUP BY + SUM)
    // Retained because the provided assessment test asserts against the raw Object[] rows.
    @Query("SELECT c.branch, SUM(a.balance) FROM Customer c JOIN c.accounts a GROUP BY c.branch")
    List<Object[]> findTotalBalancePerBranch();

    // Task 4 (type-safe variant): same aggregation exposed via an interface projection
    // so the service layer never has to cast Object[] rows. Aliases must match the
    // BranchBalance getter names.
    @Query("SELECT c.branch AS branch, SUM(a.balance) AS totalBalance " +
            "FROM Customer c JOIN c.accounts a GROUP BY c.branch")
    List<BranchBalance> findBranchBalances();

    // Task 4: Find Customers Having Multiple Accounts (COUNT + GROUP BY + HAVING)
    @Query("SELECT c FROM Customer c JOIN c.accounts a GROUP BY c HAVING COUNT(a) > 1")
    List<Customer> findCustomersWithMultipleAccounts();

    // Task 7: DTO Projection for a single customer summary
    @Query("SELECT new org.northernarc.assessment4.dto.CustomerSummaryDTO(" +
            "c.customerName, c.branch, COUNT(a), SUM(a.balance)) " +
            "FROM Customer c LEFT JOIN c.accounts a " +
            "WHERE c.customerId = :customerId " +
            "GROUP BY c.customerId, c.customerName, c.branch")
    Optional<CustomerSummaryDTO> findCustomerSummary(@Param("customerId") Long customerId);

    // Final Challenge helpers (branch and customer ranked by total balance)
    @Query("SELECT c.branch FROM Customer c JOIN c.accounts a GROUP BY c.branch ORDER BY SUM(a.balance) DESC")
    List<String> findBranchesRankedByBalance(Pageable pageable);

    @Query("SELECT c.customerName FROM Customer c JOIN c.accounts a " +
            "GROUP BY c.customerId, c.customerName ORDER BY SUM(a.balance) DESC")
    List<String> findCustomersRankedByBalance(Pageable pageable);
}
