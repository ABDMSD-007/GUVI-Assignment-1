package com.example.demo.repository;

import com.example.demo.entity.Penalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    List<Penalty> findByLoan_LoanId(Long loanId);

    long countByLoan_LoanId(Long loanId);

    // ---- Dashboard: total penalty collected ----
    @Query("SELECT COALESCE(SUM(p.penaltyAmount), 0) FROM Penalty p")
    Double getTotalPenaltyCollected();
}

