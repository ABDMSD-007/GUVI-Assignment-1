package com.example.demo.service;

import com.example.demo.dto.BranchCollectionDTO;
import com.example.demo.dto.DashboardDTO;
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
import com.example.demo.serviceimpl.LoanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    @Mock private LoanRepository loanRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private EMITransactionRepository emiRepository;
    @Mock private PenaltyRepository penaltyRepository;

    @InjectMocks private LoanServiceImpl loanService;

    private Loan loan;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loanService, "creditScoreThreshold", 700);
        loan = new Loan();
        loan.setLoanId(1L);
        loan.setLoanType(LoanType.PERSONAL);
        loan.setLoanStatus(LoanStatus.ACTIVE);
        loan.setActive(true);
    }

    // ---- getLoanById ----
    @Test
    void getLoanById_found() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        assertEquals(loan, loanService.getLoanById(1L));
    }

    @Test
    void getLoanById_notFound_throws() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(LoanNotFoundException.class, () -> loanService.getLoanById(99L));
    }

    // ---- deleteLoan = soft delete ----
    @Test
    void deleteLoan_setsInactive() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        loanService.deleteLoan(1L);
        assertFalse(loan.getActive());
        verify(loanRepository).save(loan);
    }

    @Test
    void deleteLoan_missing_throws() {
        when(loanRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(LoanNotFoundException.class, () -> loanService.deleteLoan(5L));
        verify(loanRepository, never()).save(any());
    }

    // ---- payEmi defaults ----
    @Test
    void payEmi_setsDefaults() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(emiRepository.save(any(EMITransaction.class))).thenAnswer(i -> i.getArgument(0));
        EMITransaction emi = new EMITransaction();
        EMITransaction saved = loanService.payEmi(1L, emi);
        assertEquals(PaymentStatus.PAID, saved.getPaymentStatus());
        assertEquals(LocalDate.now(), saved.getPaymentDate());
        assertEquals(loan, saved.getLoan());
    }

    // ---- latest payment edge: empty list ----
    @Test
    void getLatestPayment_empty_returnsNull() {
        when(emiRepository.findLatestPayments(any(Pageable.class))).thenReturn(Collections.emptyList());
        assertNull(loanService.getLatestPayment());
    }

    @Test
    void getLatestPayment_returnsFirst() {
        EMITransaction e = new EMITransaction();
        when(emiRepository.findLatestPayments(any(Pageable.class))).thenReturn(List.of(e));
        assertSame(e, loanService.getLatestPayment());
    }

    // ---- foreclose edge cases ----
    @Test
    void foreclose_blocksWhenPending() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(emiRepository.countByLoan_LoanIdAndPaymentStatus(1L, PaymentStatus.PENDING)).thenReturn(2L);
        assertThrows(InvalidLoanOperationException.class, () -> loanService.forecloseLoan(1L));
    }

    @Test
    void foreclose_blocksWhenMissed() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(emiRepository.countByLoan_LoanIdAndPaymentStatus(1L, PaymentStatus.PENDING)).thenReturn(0L);
        when(emiRepository.countByLoan_LoanIdAndPaymentStatus(1L, PaymentStatus.MISSED)).thenReturn(1L);
        assertThrows(InvalidLoanOperationException.class, () -> loanService.forecloseLoan(1L));
    }

    @Test
    void foreclose_closesWhenClear() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(emiRepository.countByLoan_LoanIdAndPaymentStatus(1L, PaymentStatus.PENDING)).thenReturn(0L);
        when(emiRepository.countByLoan_LoanIdAndPaymentStatus(1L, PaymentStatus.MISSED)).thenReturn(0L);
        when(loanRepository.save(loan)).thenReturn(loan);
        assertEquals(LoanStatus.CLOSED, loanService.forecloseLoan(1L).getLoanStatus());
    }

    // ---- top 10 branches: more than 10 trims; fewer keeps ----
    @Test
    void top10Branches_trimsTo10() {
        List<BranchCollectionDTO> twelve = new ArrayList<>();
        for (int i = 0; i < 12; i++) twelve.add(new BranchCollectionDTO("B" + i, (double) i));
        when(loanRepository.getBranchWiseEMICollection()).thenReturn(twelve);
        assertEquals(10, loanService.getTop10Branches().size());
    }

    @Test
    void top10Branches_keepsFewer() {
        List<BranchCollectionDTO> three = List.of(
                new BranchCollectionDTO("A", 1.0), new BranchCollectionDTO("B", 2.0), new BranchCollectionDTO("C", 3.0));
        when(loanRepository.getBranchWiseEMICollection()).thenReturn(three);
        assertEquals(3, loanService.getTop10Branches().size());
    }

    // ---- dashboard handles empty aggregates gracefully ----
    @Test
    void dashboard_emptyData() {
        when(customerRepository.count()).thenReturn(0L);
        when(loanRepository.countByLoanStatus(any(LoanStatus.class))).thenReturn(0L);
        when(emiRepository.getTotalEMICollected()).thenReturn(0.0);
        when(penaltyRepository.getTotalPenaltyCollected()).thenReturn(0.0);
        when(loanRepository.getBranchWiseEMICollection()).thenReturn(Collections.emptyList());
        when(loanRepository.findTop5CustomersByEMI(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(loanRepository.findHighestLoanAmount()).thenReturn(0.0);

        DashboardDTO d = loanService.getDashboard();
        assertNull(d.topBranch());
        assertNull(d.highestPayingCustomer());
        assertEquals(0L, d.totalCustomers());
    }

    @Test
    void dashboard_withData() {
        when(customerRepository.count()).thenReturn(12500L);
        when(loanRepository.countByLoanStatus(LoanStatus.ACTIVE)).thenReturn(8400L);
        when(loanRepository.countByLoanStatus(LoanStatus.CLOSED)).thenReturn(3600L);
        when(loanRepository.countByLoanStatus(LoanStatus.DEFAULTED)).thenReturn(124L);
        when(emiRepository.getTotalEMICollected()).thenReturn(95678000.50);
        when(penaltyRepository.getTotalPenaltyCollected()).thenReturn(782500.75);
        when(loanRepository.getBranchWiseEMICollection()).thenReturn(List.of(new BranchCollectionDTO("Bangalore", 99.0)));
        when(loanRepository.findTop5CustomersByEMI(any(Pageable.class)))
                .thenReturn(List.<Object[]>of(new Object[]{"Rahul Sharma", 50000.0}));
        when(loanRepository.findHighestLoanAmount()).thenReturn(3500000.0);

        DashboardDTO d = loanService.getDashboard();
        assertEquals("Bangalore", d.topBranch());
        assertEquals("Rahul Sharma", d.highestPayingCustomer());
        assertEquals(124L, d.defaultedLoans());
        assertEquals(3500000.0, d.highestLoanAmount());
    }

    // ---- approve & interest ----
    @Test
    void approveLoan_setsActive() {
        loan.setLoanStatus(LoanStatus.PENDING);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(loan)).thenReturn(loan);
        assertEquals(LoanStatus.ACTIVE, loanService.approveLoan(1L).getLoanStatus());
    }

    @Test
    void increaseInterest_delegates() {
        when(loanRepository.increaseInterestRate()).thenReturn(7);
        assertEquals(7, loanService.increaseInterestRate());
    }

    @Test
    void eligibleCustomers_usesThreshold() {
        when(customerRepository.findEligibleCustomers(700)).thenReturn(Collections.emptyList());
        loanService.getEligibleCustomers();
        verify(customerRepository).findEligibleCustomers(700);
    }
}


