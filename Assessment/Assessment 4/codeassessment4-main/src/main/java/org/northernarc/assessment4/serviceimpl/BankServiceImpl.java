package org.northernarc.assessment4.serviceimpl;

import org.northernarc.assessment4.dto.CustomerSummaryDTO;
import org.northernarc.assessment4.dto.DashboardResponse;
import org.northernarc.assessment4.dto.BranchBalance;
import org.northernarc.assessment4.exception.AccountNotFoundException;
import org.northernarc.assessment4.exception.CustomerNotFoundException;
import org.northernarc.assessment4.exception.DuplicateResourceException;
import org.northernarc.assessment4.model.Account;
import org.northernarc.assessment4.model.Customer;
import org.northernarc.assessment4.model.Transaction;
import org.northernarc.assessment4.repository.AccountRepository;
import org.northernarc.assessment4.repository.CustomerRepository;
import org.northernarc.assessment4.repository.TransactionRepository;
import org.northernarc.assessment4.service.BankService;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankServiceImpl implements BankService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // --- Core Entity Writing Persistence Methods ---
    @Override
    @Transactional
    public Customer saveCustomer(Customer customer) {
        // Best practice: proactively reject a duplicate email with a clean 409
        // (only for new customers) instead of relying on the DB constraint.
        if (customer.getCustomerId() == null
                && customer.getEmail() != null
                && customerRepository.findByEmail(customer.getEmail()).isPresent()) {
            throw new DuplicateResourceException("A customer already exists with email: " + customer.getEmail());
        }
        // Best practice: never persist a plain-text password, and always have a role.
        if (customer.getRole() == null || customer.getRole().isBlank()) {
            customer.setRole("USER");
        }
        String password = customer.getPassword();
        if (password != null && !password.startsWith("$2")) { // not already a BCrypt hash
            customer.setPassword(new BCryptPasswordEncoder().encode(password));
        }
        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public Account saveAccount(Account account) {
        return accountRepository.save(account);
    }

    @Override
    @Transactional
    public void deleteAccount(String accountNumber) {
        if (!accountRepository.existsById(accountNumber)) {
            throw new AccountNotFoundException("Account not found with number: " + accountNumber);
        }
        accountRepository.deleteById(accountNumber);
    }

    // --- Task 3: Spring Data JPA Derived Queries ---
    @Override
    public List<Account> getAccountsByType(String accountType) {
        return accountRepository.findByAccountType(accountType);
    }

    @Override
    public List<Customer> getCustomersByBranch(String branch) {
        return customerRepository.findByBranch(branch);
    }

    @Override
    public List<Transaction> getTransactionsByType(String transactionType) {
        return transactionRepository.findByTransactionType(transactionType);
    }

    @Override
    public List<Account> getAccountsWithBalanceGreaterThan(double amount) {
        return accountRepository.findByBalanceGreaterThan(amount);
    }

    // --- Task 4: JPQL Custom Queries ---
    @Override
    public List<Customer> getRichCustomers(double threshold) {
        return customerRepository.findRichCustomers(threshold);
    }

    @Override
    public Map<String, Double> getTotalBalancePerBranch() {
        Map<String, Double> result = new LinkedHashMap<>();
        for (BranchBalance row : customerRepository.findBranchBalances()) {
            Double total = row.getTotalBalance() == null ? 0.0 : row.getTotalBalance();
            result.put(row.getBranch(), total);
        }
        return result;
    }

    @Override
    public List<Customer> getCustomersWithMultipleAccounts() {
        return customerRepository.findCustomersWithMultipleAccounts();
    }

    @Override
    public Transaction getLatestTransaction() {
        List<Transaction> latest = transactionRepository.findLatestTransaction(PageRequest.of(0, 1));
        return latest.isEmpty() ? null : latest.get(0);
    }

    @Override
    public List<Account> getAccountsWithNoTransactions() {
        return accountRepository.findAccountsWithNoTransactions();
    }

    // --- Task 5: JPQL Update Query ---
    @Override
    @Transactional
    public void increaseAccountBalance(String accountNumber, double amount) {
        int updated = accountRepository.increaseBalance(accountNumber, amount);
        if (updated == 0) {
            throw new AccountNotFoundException("Account not found with number: " + accountNumber);
        }
    }

    // --- Task 6: Pagination & Sorting ---
    @Override
    public Page<Account> getAllAccountsPaginated(Pageable pageable) {
        return accountRepository.findAll(pageable);
    }

    // --- Task 7: DTO Projection Mapping ---
    @Override
    public CustomerSummaryDTO getCustomerSummary(Long customerId) {
        return customerRepository.findCustomerSummary(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + customerId));
    }

    // --- Final Challenge: Optimized Dashboard Metrics ---
    @Override
    public DashboardResponse getDashboardMetrics() {
        long totalCustomers = customerRepository.count();
        long totalAccounts = accountRepository.count();

        Double totalBalance = accountRepository.findTotalBalance();
        if (totalBalance == null) {
            totalBalance = 0.0;
        }

        Pageable topOne = PageRequest.of(0, 1);
        List<String> topBranches = customerRepository.findBranchesRankedByBalance(topOne);
        String topBranch = topBranches.isEmpty() ? null : topBranches.get(0);

        List<String> topCustomers = customerRepository.findCustomersRankedByBalance(topOne);
        String highestBalanceCustomer = topCustomers.isEmpty() ? null : topCustomers.get(0);

        return new DashboardResponse(totalCustomers, totalAccounts, totalBalance, topBranch, highestBalanceCustomer);
    }
}
