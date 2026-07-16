package com.example.demo.controller;

import com.example.demo.dto.BranchCollectionDTO;
import com.example.demo.dto.CustomerSummaryDTO;
import com.example.demo.dto.MonthlyCollectionDTO;
import com.example.demo.entity.Customer;
import com.example.demo.entity.EMITransaction;
import com.example.demo.entity.Loan;
import com.example.demo.enums.LoanType;
import com.example.demo.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loans")
@Slf4j
@Tag(name = "Loans", description = "Loan lifecycle, EMI payments and analytics endpoints")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // Task 6 - Pagination & Sorting, default emiAmount DESC. USER can view.
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "List loans", description = "Paginated & sorted (emiAmount DESC by default). Requires USER role.")
    public ResponseEntity<Page<Loan>> getLoans(
            @PageableDefault(size = 10, sort = "emiAmount", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("GET /loans - fetching loans page={} size={}", pageable.getPageNumber(), pageable.getPageSize());
        return ResponseEntity.ok(loanService.getAllLoans(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get loan by id")
    public ResponseEntity<Loan> getLoan(@PathVariable Long id) {
        log.info("GET /loans/{} - fetching loan", id);
        return ResponseEntity.ok(loanService.getLoanById(id));
    }

    // Task 9 - USER: View EMI schedule, Pay EMI
    @GetMapping("/{id}/schedule")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get EMI schedule for a loan")
        public ResponseEntity<List<EMITransaction>> schedule(@PathVariable Long id) {
            log.info("GET /loans/{}/schedule - fetching EMI schedule", id);
            return ResponseEntity.ok(loanService.getEmiSchedule(id));
        }

        @PostMapping("/{id}/pay")
        @PreAuthorize("hasRole('USER')")
        @Operation(summary = "Pay an EMI for a loan")
        public ResponseEntity<EMITransaction> payEmi(@PathVariable Long id, @RequestBody EMITransaction emi) {
            log.info("POST /loans/{}/pay - recording EMI payment installment={}", id, emi.getInstallmentNumber());
            return ResponseEntity.ok(loanService.payEmi(id, emi));
    }

    // Task 9 - MANAGER: approve loan & update interest rate
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Approve a loan", description = "Sets loan status to ACTIVE. Requires MANAGER role.")
    public ResponseEntity<Loan> approve(@PathVariable Long id) {
        log.info("PUT /loans/{}/approve - approving loan", id);
        return ResponseEntity.ok(loanService.approveLoan(id));
    }

    @PutMapping("/increase-interest")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Increase interest", description = "Increases interest rate by 0.5% on PERSONAL loans. Requires MANAGER role.")
    public ResponseEntity<String> increaseInterest() {
        int count = loanService.increaseInterestRate();
        log.info("PUT /loans/increase-interest - interest increased for {} personal loans", count);
        return ResponseEntity.ok("Interest increased for " + count + " personal loans");
    }

    // Task 9 - ADMIN: delete loan
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete (soft) a loan", description = "Soft-deletes a loan. Requires ADMIN role.")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        log.warn("DELETE /loans/{} - soft deleting loan", id);
        loanService.deleteLoan(id);
        return ResponseEntity.ok("Loan deleted successfully");
    }

    // Task 3 + 4 query endpoints
    @GetMapping("/type/{type}")
    @Operation(summary = "Get loans by type")
    public ResponseEntity<List<Loan>> byType(@PathVariable LoanType type) {
        log.info("GET /loans/type/{}", type);
        return ResponseEntity.ok(loanService.getLoansByType(type));
    }

    @GetMapping("/customers/branch/{branch}")
    @Operation(summary = "Get customers by branch")
    public ResponseEntity<List<Customer>> byBranch(@PathVariable String branch) {
        log.info("GET /loans/customers/branch/{}", branch);
        return ResponseEntity.ok(loanService.getCustomersByBranch(branch));
    }

    @GetMapping("/customers/min-loans/{n}")
    @Operation(summary = "Customers with more than N loans")
    public ResponseEntity<List<Customer>> customersWithMinLoans(@PathVariable long n) {
        log.info("GET /loans/customers/min-loans/{}", n);
        return ResponseEntity.ok(loanService.getCustomersWithMoreThanNLoans(n));
    }

    @GetMapping("/branch-collection")
    @Operation(summary = "EMI collection grouped by branch")
    public ResponseEntity<List<BranchCollectionDTO>> branchCollection() {
        log.info("GET /loans/branch-collection");
        return ResponseEntity.ok(loanService.getBranchWiseCollection());
    }

    @GetMapping("/customers/multiple-loan-types")
    @Operation(summary = "Customers holding multiple loan types")
    public ResponseEntity<List<Customer>> multiLoanTypes() {
        log.info("GET /loans/customers/multiple-loan-types");
        return ResponseEntity.ok(loanService.getCustomersWithMultipleLoanTypes());
    }

    @GetMapping("/latest-payment")
    @Operation(summary = "Latest EMI payment")
    public ResponseEntity<EMITransaction> latestPayment() {
        log.info("GET /loans/latest-payment");
        return ResponseEntity.ok(loanService.getLatestPayment());
    }

    @GetMapping("/no-penalty")
    @Operation(summary = "Loans with no penalties")
    public ResponseEntity<List<Loan>> noPenalty() {
        log.info("GET /loans/no-penalty");
        return ResponseEntity.ok(loanService.getLoansWithoutPenalty());
    }

    @GetMapping("/top5-emi")
    @Operation(summary = "Top 5 customers by EMI paid")
    public ResponseEntity<List<Object[]>> top5() {
        log.info("GET /loans/top5-emi");
        return ResponseEntity.ok(loanService.getTop5CustomersByEMI());
    }

    // Task 7
    @GetMapping("/customer-summaries")
    @Operation(summary = "Customer summary projection")
    public ResponseEntity<List<CustomerSummaryDTO>> summaries() {
        log.info("GET /loans/customer-summaries");
        return ResponseEntity.ok(loanService.getCustomerSummaries());
    }

    // Bonus endpoints
    @GetMapping("/overdue")
    @Operation(summary = "Overdue loans (pending EMIs > 30 days)")
    public ResponseEntity<List<Loan>> overdue() {
        log.info("GET /loans/overdue");
        return ResponseEntity.ok(loanService.getOverdueLoans());
    }

    @PutMapping("/{id}/foreclose")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Foreclose a loan", description = "Closes a loan if no pending/missed EMIs. Requires MANAGER role.")
    public ResponseEntity<Loan> foreclose(@PathVariable Long id) {
        log.info("PUT /loans/{}/foreclose - attempting foreclosure", id);
        return ResponseEntity.ok(loanService.forecloseLoan(id));
    }

    @GetMapping("/top10-branches")
    @Operation(summary = "Top 10 branches by collection")
    public ResponseEntity<List<BranchCollectionDTO>> top10() {
        log.info("GET /loans/top10-branches");
        return ResponseEntity.ok(loanService.getTop10Branches());
    }

    @GetMapping("/eligible-customers")
    @Operation(summary = "Credit-score eligible customers")
    public ResponseEntity<List<Customer>> eligible() {
        log.info("GET /loans/eligible-customers");
        return ResponseEntity.ok(loanService.getEligibleCustomers());
    }

    @GetMapping("/monthly-report")
    @Operation(summary = "Monthly collection report")
    public ResponseEntity<List<MonthlyCollectionDTO>> monthly() {
        log.info("GET /loans/monthly-report");
        return ResponseEntity.ok(loanService.getMonthlyReport());
    }
}

