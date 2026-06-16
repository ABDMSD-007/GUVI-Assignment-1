package dao;

import entity.Loan;

import java.sql.SQLException;
import java.util.Collection;

public interface LoanDao {

    int save(Loan loan) throws SQLException;

    Loan findById(int id) throws SQLException;

    Collection<Loan> findAll() throws SQLException;

    Collection<Loan> findByType(String type) throws SQLException;

    void updateById(int id, Loan loan) throws SQLException;

    void deleteById(int id) throws SQLException;
}