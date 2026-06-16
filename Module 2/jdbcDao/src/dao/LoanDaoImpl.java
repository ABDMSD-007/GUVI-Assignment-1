package dao;

import connection.DBManager;
import entity.Loan;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LoanDaoImpl implements LoanDao {

    @Override
    public int save(Loan loan) throws SQLException {

        Connection con = DBManager.getConnection();

        String sql =
                "insert into loan(id,amount,interest,duration,loantype,loanstatus) values(?,?,?,?,?,?)";

        PreparedStatement stmt = con.prepareStatement(sql);

        stmt.setInt(1, loan.getId());
        stmt.setInt(2, loan.getAmount());
        stmt.setInt(3, loan.getInterest());
        stmt.setInt(4, loan.getDuration());
        stmt.setString(5, loan.getLoanType());
        stmt.setString(6, loan.getLoanStatus());

        int rows = stmt.executeUpdate();

        DBManager.closeConnection(con);

        return rows;
    }

    @Override
    public Loan findById(int id) throws SQLException {

        Connection con = DBManager.getConnection();

        String sql = "select * from loan where id=?";

        PreparedStatement stmt = con.prepareStatement(sql);

        stmt.setInt(1, id);

        ResultSet rs = stmt.executeQuery();

        if (rs.next())
            return mapToLoan(rs);

        DBManager.closeConnection(con);

        return null;
    }

    @Override
    public Collection<Loan> findAll() throws SQLException {

        Connection con = DBManager.getConnection();

        String sql = "select * from loan";

        PreparedStatement stmt = con.prepareStatement(sql);

        ResultSet rs = stmt.executeQuery();

        List<Loan> loans = new ArrayList<>();

        while (rs.next()) {
            loans.add(mapToLoan(rs));
        }

        DBManager.closeConnection(con);

        return loans;
    }

    @Override
    public Collection<Loan> findByType(String type) throws SQLException {

        Connection con = DBManager.getConnection();

        String sql = "select * from loan where loantype=?";

        PreparedStatement stmt = con.prepareStatement(sql);

        stmt.setString(1, type);

        ResultSet rs = stmt.executeQuery();

        List<Loan> loans = new ArrayList<>();

        while (rs.next()) {
            loans.add(mapToLoan(rs));
        }

        DBManager.closeConnection(con);

        return loans;
    }

    @Override
    public void updateById(int id, Loan loan) throws SQLException {

        Connection con = DBManager.getConnection();

        String sql =
                "update loan set amount=?,interest=?,duration=?,loantype=?,loanstatus=? where id=?";

        PreparedStatement stmt = con.prepareStatement(sql);

        stmt.setInt(1, loan.getAmount());
        stmt.setInt(2, loan.getInterest());
        stmt.setInt(3, loan.getDuration());
        stmt.setString(4, loan.getLoanType());
        stmt.setString(5, loan.getLoanStatus());
        stmt.setInt(6, id);

        stmt.executeUpdate();

        DBManager.closeConnection(con);
    }

    @Override
    public void deleteById(int id) throws SQLException {

        Connection con = DBManager.getConnection();

        String sql = "delete from loan where id=?";

        PreparedStatement stmt = con.prepareStatement(sql);

        stmt.setInt(1, id);

        stmt.executeUpdate();

        DBManager.closeConnection(con);
    }

    private Loan mapToLoan(ResultSet rs) throws SQLException {

        return new Loan(
                rs.getInt("id"),
                rs.getInt("amount"),
                rs.getInt("interest"),
                rs.getInt("duration"),
                rs.getString("loantype"),
                rs.getString("loanstatus")
        );
    }
}