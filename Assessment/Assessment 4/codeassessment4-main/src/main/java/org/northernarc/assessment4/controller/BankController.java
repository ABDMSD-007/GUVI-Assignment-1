package org.northernarc.assessment4.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.northernarc.assessment4.dto.CustomerSummaryDTO;
import org.northernarc.assessment4.dto.DashboardResponse;
import org.northernarc.assessment4.model.Account;
import org.northernarc.assessment4.model.Customer;
import org.northernarc.assessment4.model.Transaction;
import org.northernarc.assessment4.service.BankService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class BankController {

    private final BankService bankService;

    // --- Core Onboarding & Setup Endpoints ---
    @PostMapping("/customers")
    public ResponseEntity<Customer> createCustomer(@Valid @RequestBody Customer customer) {
        return new ResponseEntity<>(bankService.saveCustomer(customer), HttpStatus.CREATED);
    }

    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@Valid @RequestBody Account account) {
        return new ResponseEntity<>(bankService.saveAccount(account), HttpStatus.CREATED);
    }

    // --- Task 6: Pagination & Sorting (Default: Balance DESC) ---
    @GetMapping("/accounts")
    public ResponseEntity<Page<Account>> getAllAccounts(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page index must be zero or positive") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Page size must be at least 1") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("balance").descending());
        return ResponseEntity.ok(bankService.getAllAccountsPaginated(pageable));
    }

    // --- Task 9: Role Based Access Control Endpoints ---
    @DeleteMapping("/accounts/{accountNumber}")
    @PreAuthorize("hasRole('ADMIN')") // Task 9: ADMIN can delete accounts
    public ResponseEntity<Void> deleteAccount(
            @PathVariable @NotBlank(message = "Account number is required") String accountNumber) {
        bankService.deleteAccount(accountNumber);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/accounts/{accountNumber}/balance")
    @PreAuthorize("hasRole('MANAGER')") // Task 9: MANAGER can update accounts
    public ResponseEntity<Void> updateAccountBalance(
            @PathVariable @NotBlank(message = "Account number is required") String accountNumber,
            @RequestParam @Positive(message = "Amount must be positive") double amount) {
        bankService.increaseAccountBalance(accountNumber, amount);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/accounts/view/{accountNumber}")
    @PreAuthorize("hasRole('USER')") // Task 9: USER can only view accounts
    public ResponseEntity<List<Account>> viewAccountsByType(
            @RequestParam @NotBlank(message = "Account type is required") String type) {
        return ResponseEntity.ok(bankService.getAccountsByType(type));
    }

    // --- Task 7: DTO Projection Aggregation ---
    @GetMapping("/customers/{customerId}/summary")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<CustomerSummaryDTO> getCustomerSummary(
            @PathVariable @NotNull(message = "Customer id is required") Long customerId) {
        return ResponseEntity.ok(bankService.getCustomerSummary(customerId));
    }

    // --- Task 3 & 4: Query Operations ---
    @GetMapping("/customers/rich")
    public ResponseEntity<List<Customer>> getRichCustomers(
            @RequestParam @PositiveOrZero(message = "Threshold must be zero or positive") double threshold) {
        return ResponseEntity.ok(bankService.getRichCustomers(threshold));
    }

    @GetMapping("/branches/balances")
    public ResponseEntity<Map<String, Double>> getBranchBalances() {
        return ResponseEntity.ok(bankService.getTotalBalancePerBranch());
    }

    @GetMapping("/customers/multiple-accounts")
    public ResponseEntity<List<Customer>> getCustomersWithMultipleAccounts() {
        return ResponseEntity.ok(bankService.getCustomersWithMultipleAccounts());
    }

    @GetMapping("/transactions/latest")
    public ResponseEntity<Transaction> getLatestTransaction() {
        return ResponseEntity.ok(bankService.getLatestTransaction());
    }

    @GetMapping("/accounts/idle")
    public ResponseEntity<List<Account>> getIdleAccounts() {
        return ResponseEntity.ok(bankService.getAccountsWithNoTransactions());
    }

    // --- Final Challenge: Metrics Dashboard Generation ---
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboardMetrics() {
        return ResponseEntity.ok(bankService.getDashboardMetrics());
    }
}
