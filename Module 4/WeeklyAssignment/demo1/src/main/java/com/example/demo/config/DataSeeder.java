package com.example.demo.config;

import com.example.demo.entity.Customer;
import com.example.demo.entity.EMITransaction;
import com.example.demo.entity.Loan;
import com.example.demo.entity.Penalty;
import com.example.demo.enums.LoanStatus;
import com.example.demo.enums.LoanType;
import com.example.demo.enums.PaymentMode;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.enums.Role;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Java-only data seeder (no SQL / no Python).
 *
 * <p>Creates an admin, a manager and a few users, plus a complete set of loans,
 * EMI transactions and penalties so the whole API can be exercised.</p>
 *
 * <p>Disabled by default. Enable it for a single run with:
 * {@code app.seed.enabled=true} in application.properties, or via
 * {@code --app.seed.enabled=true} on the command line / an env var.</p>
 *
 * <p>Idempotent: customers are matched by email and skipped if present;
 * loans are only created for a customer that has none yet.</p>
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final double PENALTY_PER_MISS = 500.0;

    private final CustomerRepository customerRepository;
    private final LoanRepository loanRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(CustomerRepository customerRepository,
                      LoanRepository loanRepository,
                      PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.loanRepository = loanRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("DataSeeder starting (app.seed.enabled=true)...");

        upsertCustomer("Admin User", "admin@bank.com", "Admin@123", "9000000001", "Head Office", 800, Role.ADMIN);
        Customer priya   = upsertCustomer("Priya Menon",  "priya@bank.com",  "Manager@123", "9000000002", "Bengaluru",   780, Role.MANAGER);
        Customer amit    = upsertCustomer("Amit Verma",   "amit@bank.com",   "User@123",    "9000000003", "Mumbai",      720, Role.USER);
        Customer sneha   = upsertCustomer("Sneha Nair",   "sneha@bank.com",  "User@123",    "9000000004", "Chennai",     690, Role.USER);
        Customer vikram  = upsertCustomer("Vikram Singh", "vikram@bank.com", "User@123",    "9000000005", "Delhi",       750, Role.USER);

        // ---- Amit: HOME (active), PERSONAL (missed+penalty), VEHICLE (closed), soft-deleted ----
        seedLoanIfNone(amit, LoanType.HOME, 2_000_000, 8.5, 240, LoanStatus.ACTIVE, true,
                LocalDate.of(2025, 8, 5),
                new Installment[]{
                        pay(PaymentMode.NETBANKING), pay(PaymentMode.NETBANKING), pay(PaymentMode.NETBANKING),
                        pay(PaymentMode.NETBANKING), pay(PaymentMode.NETBANKING), pay(PaymentMode.NETBANKING)});
        seedLoanIfNone(amit, LoanType.PERSONAL, 300_000, 12.0, 24, LoanStatus.ACTIVE, true,
                LocalDate.of(2025, 10, 5),
                new Installment[]{
                        pay(PaymentMode.UPI), pay(PaymentMode.UPI), miss(PaymentMode.UPI),
                        pay(PaymentMode.CARD), pending(PaymentMode.UPI)});
        seedLoanIfNone(amit, LoanType.VEHICLE, 800_000, 9.5, 12, LoanStatus.CLOSED, true,
                LocalDate.of(2024, 6, 5), allPaid(12, PaymentMode.CARD));
        // soft-deleted loan (active=false) to test the active flag filter
        seedLoanIfNone(amit, LoanType.PERSONAL, 100_000, 13.0, 12, LoanStatus.CLOSED, false,
                LocalDate.of(2024, 1, 5), new Installment[]{});

        // ---- Sneha: EDUCATION (active w/ pending), PERSONAL (defaulted, missed) ----
        seedLoanIfNone(sneha, LoanType.EDUCATION, 500_000, 9.0, 60, LoanStatus.ACTIVE, true,
                LocalDate.of(2025, 9, 5),
                new Installment[]{
                        pay(PaymentMode.NETBANKING), pay(PaymentMode.NETBANKING),
                        pay(PaymentMode.UPI), pending(PaymentMode.UPI)});
        seedLoanIfNone(sneha, LoanType.PERSONAL, 150_000, 13.0, 12, LoanStatus.DEFAULTED, true,
                LocalDate.of(2025, 6, 5),
                new Installment[]{pay(PaymentMode.UPI), miss(PaymentMode.UPI), miss(PaymentMode.UPI)});

        // ---- Vikram: VEHICLE (active), HOME (pending, no EMIs) ----
        seedLoanIfNone(vikram, LoanType.VEHICLE, 600_000, 9.5, 48, LoanStatus.ACTIVE, true,
                LocalDate.of(2025, 11, 5),
                new Installment[]{pay(PaymentMode.CASH), pay(PaymentMode.CARD), pay(PaymentMode.UPI)});
        seedLoanIfNone(vikram, LoanType.HOME, 3_000_000, 8.4, 300, LoanStatus.PENDING, true,
                LocalDate.of(2026, 7, 5), new Installment[]{});

        // ---- Priya (manager): PERSONAL (active) ----
        seedLoanIfNone(priya, LoanType.PERSONAL, 400_000, 11.0, 36, LoanStatus.ACTIVE, true,
                LocalDate.of(2026, 1, 5),
                new Installment[]{pay(PaymentMode.NETBANKING), pay(PaymentMode.NETBANKING)});

        // admin gets no loans on purpose
        log.info("DataSeeder finished. Total customers={}, total loans={}",
                customerRepository.count(), loanRepository.count());
    }

    // ---------------------------------------------------------------------
    // Customer helper
    // ---------------------------------------------------------------------
    private Customer upsertCustomer(String name, String email, String rawPassword,
                                    String mobile, String branch, int creditScore, Role role) {
        return customerRepository.findByEmail(email).orElseGet(() -> {
            Customer c = new Customer();
            c.setCustomerName(name);
            c.setEmail(email);
            c.setPassword(passwordEncoder.encode(rawPassword));
            c.setMobileNumber(mobile);
            c.setBranchName(branch);
            c.setCreditScore(creditScore);
            c.setRole(role);
            Customer saved = customerRepository.save(c);
            log.info("Seeded customer {} ({}) with role {}", name, email, role);
            return saved;
        });
    }

    // ---------------------------------------------------------------------
    // Loan helper
    // ---------------------------------------------------------------------
    private void seedLoanIfNone(Customer customer, LoanType type, double principal, double annualRate,
                                int tenureMonths, LoanStatus status, boolean active,
                                LocalDate firstDueDate, Installment[] installments) {
        boolean alreadyHasLoans = customer.getLoans() != null && !customer.getLoans().isEmpty();
        if (alreadyHasLoans) {
            log.debug("Skipping loans for {} (already has loans)", customer.getEmail());
            return;
        }

        double emi = calculateEmi(principal, annualRate, tenureMonths);

        Loan loan = new Loan();
        loan.setLoanType(type);
        loan.setPrincipalAmount(principal);
        loan.setInterestRate(annualRate);
        loan.setTenureMonths(tenureMonths);
        loan.setEmiAmount(emi);
        loan.setLoanStatus(status);
        loan.setActive(active);
        loan.setCustomer(customer);

        for (int i = 0; i < installments.length; i++) {
            Installment inst = installments[i];
            int installmentNumber = i + 1;
            LocalDate dueDate = firstDueDate.plusMonths(i);

            EMITransaction tx = new EMITransaction();
            tx.setInstallmentNumber(installmentNumber);
            tx.setAmountPaid(inst.status() == PaymentStatus.PAID ? emi : 0.0);
            tx.setPaymentDate(dueDate);
            tx.setPaymentMode(inst.mode());
            tx.setPaymentStatus(inst.status());
            tx.setLoan(loan);
            loan.getEmiTransactions().add(tx);

            if (inst.status() == PaymentStatus.MISSED) {
                Penalty penalty = new Penalty();
                penalty.setPenaltyAmount(PENALTY_PER_MISS);
                penalty.setReason("Late/missed payment for installment " + installmentNumber);
                penalty.setPenaltyDate(dueDate);
                penalty.setLoan(loan);
                loan.getPenalties().add(penalty);
            }
        }

        loanRepository.save(loan); // cascades EMI transactions + penalties
        log.info("Seeded {} loan (id will be generated) for {} - principal={}, emi={}, status={}",
                type, customer.getEmail(), principal, emi, status);
    }

    /** Standard reducing-balance EMI: P*r*(1+r)^n / ((1+r)^n - 1). */
    private double calculateEmi(double principal, double annualRatePercent, int months) {
        double r = annualRatePercent / 12.0 / 100.0;
        if (r == 0) {
            return round2(principal / months);
        }
        double factor = Math.pow(1 + r, months);
        return round2(principal * r * factor / (factor - 1));
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ---------------------------------------------------------------------
    // Small value holder for an installment plan entry
    // ---------------------------------------------------------------------
    private record Installment(PaymentStatus status, PaymentMode mode) {
    }

    private static Installment pay(PaymentMode mode)     { return new Installment(PaymentStatus.PAID, mode); }
    private static Installment miss(PaymentMode mode)    { return new Installment(PaymentStatus.MISSED, mode); }
    private static Installment pending(PaymentMode mode) { return new Installment(PaymentStatus.PENDING, mode); }

    private static Installment[] allPaid(int count, PaymentMode mode) {
        Installment[] arr = new Installment[count];
        for (int i = 0; i < count; i++) {
            arr[i] = pay(mode);
        }
        return arr;
    }
}





