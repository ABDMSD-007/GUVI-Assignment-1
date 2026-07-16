package com.example.demo.serviceimpl;

import com.example.demo.dto.BranchCollectionDTO;
import com.example.demo.dto.CustomerSummaryDTO;
import com.example.demo.dto.DashboardDTO;
import com.example.demo.dto.MonthlyCollectionDTO;
import com.example.demo.entity.Customer;
import com.example.demo.entity.EMITransaction;
import com.example.demo.entity.Loan;
import com.example.demo.enums.LoanStatus;
import com.example.demo.enums.LoanType;
import com.example.demo.enums.PaymentStatus;
import com.example.demo.exception.InvalidLoanOperationException;
import com.example.demo.exception.LoanNotFoundException;
import com.example.demo.repository.CustomerRepository;
import com.example.demo.repository.EMITransactionRepository;
import com.example.demo.repository.LoanRepository;
import com.example.demo.repository.PenaltyRepository;
import com.example.demo.service.LoanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    private final EMITransactionRepository emiRepository;
    private final PenaltyRepository penaltyRepository;

    @Value("${app.loan.credit-score-threshold:700}")
    private int creditScoreThreshold;

    public LoanServiceImpl(LoanRepository loanRepository,
                           CustomerRepository customerRepository,
                           EMITransactionRepository emiRepository,
                           PenaltyRepository penaltyRepository) {
        this.loanRepository = loanRepository;
        this.customerRepository = customerRepository;
        this.emiRepository = emiRepository;
        this.penaltyRepository = penaltyRepository;
    }

    @Override
    public Page<Loan> getAllLoans(Pageable pageable) {
        return loanRepository.findAllActive(pageable);
    }

    @Override
    public Loan getLoanById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Loan not found with id: {}", id);
                    return new LoanNotFoundException("Loan not found with id: " + id);
                });
    }

    @Override
    @Transactional
    public void deleteLoan(Long id) {
        Loan loan = getLoanById(id);
        loan.setActive(false); // soft delete
        loanRepository.save(loan);
        log.info("Soft-deleted loan id={}", id);
    }

    @Override
    @Transactional
    public Loan approveLoan(Long id) {
        Loan loan = getLoanById(id);
        loan.setLoanStatus(LoanStatus.ACTIVE);
        log.info("Approved loan id={} (status=ACTIVE)", id);
        return loanRepository.save(loan);
    }

    @Override
    public int increaseInterestRate() {
        int count = loanRepository.increaseInterestRate();
        log.info("Increased interest rate on {} PERSONAL loans", count);
        return count;
    }

    @Override
    @Transactional
    public EMITransaction payEmi(Long loanId, EMITransaction emi) {
        Loan loan = getLoanById(loanId);
        emi.setLoan(loan);
        if (emi.getPaymentDate() == null) emi.setPaymentDate(LocalDate.now());
        if (emi.getPaymentStatus() == null) emi.setPaymentStatus(PaymentStatus.PAID);
        EMITransaction saved = emiRepository.save(emi);
        log.info("Recorded EMI payment for loan id={} amount={} status={}",
                loanId, saved.getAmountPaid(), saved.getPaymentStatus());
        return saved;
    }

    @Override
    public List<EMITransaction> getEmiSchedule(Long loanId) {
        return emiRepository.findByLoan_LoanId(loanId);
    }

    @Override
    public List<Loan> getLoansByType(LoanType type) {
        return loanRepository.findByLoanType(type);
    }

    @Override
    public List<Customer> getCustomersByBranch(String branch) {
        return customerRepository.findByBranchName(branch);
    }

    @Override
    public List<Customer> getCustomersWithMoreThanNLoans(long minimumLoans) {
        return customerRepository.findCustomersWithMoreThanNLoans(minimumLoans);
    }

    @Override
    public List<BranchCollectionDTO> getBranchWiseCollection() {
        return loanRepository.getBranchWiseEMICollection();
    }

    @Override
    public List<Customer> getCustomersWithMultipleLoanTypes() {
        return customerRepository.findCustomersWithMultipleLoanTypes();
    }

    @Override
    public EMITransaction getLatestPayment() {
        List<EMITransaction> list = emiRepository.findLatestPayments(PageRequest.of(0, 1));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public List<Loan> getLoansWithoutPenalty() {
        return loanRepository.findLoansWithNoPenalty();
    }

    @Override
    public List<Object[]> getTop5CustomersByEMI() {
        return loanRepository.findTop5CustomersByEMI(PageRequest.of(0, 5));
    }

    @Override
    public List<CustomerSummaryDTO> getCustomerSummaries() {
        return customerRepository.getCustomerSummaries();
    }

    @Override
    public DashboardDTO getDashboard() {
        List<BranchCollectionDTO> branches = loanRepository.getBranchWiseEMICollection();
        List<Object[]> topCustomers = loanRepository.findTop5CustomersByEMI(PageRequest.of(0, 1));
        return DashboardDTO.builder()
                .totalCustomers(customerRepository.count())
                .activeLoans(loanRepository.countByLoanStatus(LoanStatus.ACTIVE))
                .closedLoans(loanRepository.countByLoanStatus(LoanStatus.CLOSED))
                .totalEMICollected(emiRepository.getTotalEMICollected())
                .totalPenaltyCollected(penaltyRepository.getTotalPenaltyCollected())
                .topBranch(branches.isEmpty() ? null : branches.get(0).branchName())
                .highestPayingCustomer(topCustomers.isEmpty() ? null : (String) topCustomers.get(0)[0])
                .highestLoanAmount(loanRepository.findHighestLoanAmount())
                .defaultedLoans(loanRepository.countByLoanStatus(LoanStatus.DEFAULTED))
                .build();
    }

    @Override
    public List<Loan> getOverdueLoans() {
        return loanRepository.findOverdueLoans(LocalDate.now().minusDays(30));
    }

    @Override
    @Transactional
    public Loan forecloseLoan(Long id) {
        Loan loan = getLoanById(id);
        long pending = emiRepository.countByLoan_LoanIdAndPaymentStatus(id, PaymentStatus.PENDING);
        long missed = emiRepository.countByLoan_LoanIdAndPaymentStatus(id, PaymentStatus.MISSED);
        if (pending > 0 || missed > 0) {
            log.warn("Foreclosure blocked for loan id={} (pending={}, missed={})", id, pending, missed);
            throw new InvalidLoanOperationException("Cannot foreclose: pending/missed EMIs exist");
        }
        loan.setLoanStatus(LoanStatus.CLOSED);
        log.info("Foreclosed loan id={} (status=CLOSED)", id);
        return loanRepository.save(loan);
    }

    @Override
    public List<BranchCollectionDTO> getTop10Branches() {
        List<BranchCollectionDTO> all = loanRepository.getBranchWiseEMICollection();
        return all.size() > 10 ? all.subList(0, 10) : all;
    }

    @Override
    public List<Customer> getEligibleCustomers() {
        return customerRepository.findEligibleCustomers(creditScoreThreshold);
    }

    @Override
    public List<MonthlyCollectionDTO> getMonthlyReport() {
        return emiRepository.getMonthlyCollectionReport();
    }
}


