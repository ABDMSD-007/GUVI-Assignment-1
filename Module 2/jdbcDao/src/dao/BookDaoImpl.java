package dao;

import connection.DBManager;
import entity.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public class BookDaoImpl implements BookDao
{

    @Override
    public int save(Book book) throws SQLException {
        Connection con= DBManager.getConnection();
        String sql = "INSERT INTO book (title,author,publisher) VALUES (?,?,?)";
        PreparedStatement stmt =con.prepareStatement(sql);
        stmt.setString(1, book.getTitle());
        stmt.setString(2, book.getAuthor());
        stmt.setString(3, book.getPublisher());
        int rows=stmt.executeUpdate();
        DBManager.closeConnection(con);
        return rows;
    }

    @Override
    public Book findById(int id) throws SQLException {
        Connection con=DBManager.getConnection();
        String sql="Select * from book where ID = ?";
        PreparedStatement stmt = con.prepareStatement(sql);
        stmt.setInt(1,id);
        ResultSet rs=stmt.executeQuery();
        if (rs.next())
            return mapToBook(rs);
        DBManager.closeConnection(con);
        return null;
    }

    private Book mapToBook(ResultSet rs) throws SQLException
    {
        return new Book(rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("author"),
                        rs.getString("publisher"));
    }

    @Override
    public void deleteById(int id) throws SQLException {
        Connection con=DBManager.getConnection();
        String sql="Delete * from book where ID = ?";
        PreparedStatement stmt = con.prepareStatement(sql);
        stmt.setInt(1,id);
        ResultSet rs=stmt.executeQuery();
        DBManager.closeConnection(con);
    }

    @Override
    public void updateById(int id, Book book) throws SQLException {

    }

    @Override
    public void deleteAll() throws SQLException {

    }

    @Override
    public Collection<Book> findAll() throws SQLException {
        return List.of();
    }

    @Override
    public Collection<Book> findbyAuthor(String author) throws SQLException {
        return List.of();
    }

    @Override
    public Collection<Book> findbyTitle(String author) throws SQLException {
        return List.of();
    }

    @Override
    public Collection<Book> findbyPublisher(String publisher) throws SQLException {
        return List.of();
    }

    @Override
    public Collection<Book> findSortedByTitleAsc(String author) throws SQLException {
        return List.of();
    }

    @Override
    public Collection<Book> findSortedByTitleDesc(String author) throws SQLException {
        return List.of();
    }

    @Override
    public Collection<Book> findSortedByTitleAndPublisher(String author, String publisher) throws SQLException {
        return List.of();
    }


}
