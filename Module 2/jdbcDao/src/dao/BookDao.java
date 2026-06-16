package dao;

import entity.Book;

import java.sql.SQLException;
import java.util.Collection;

public interface BookDao
{
    public int save(Book book) throws SQLException;
    public Book findById(int id)throws SQLException;
    public void deleteById(int id)throws SQLException;
    public void updateById(int id,Book book)throws SQLException;
    public void deleteAll()throws SQLException;
    public Collection<Book> findAll()throws SQLException;
    public Collection<Book> findbyAuthor(String author)throws SQLException;
    public Collection<Book> findbyTitle(String author)throws SQLException;
    public Collection<Book> findbyPublisher(String publisher)throws SQLException;
    public Collection<Book> findSortedByTitleAsc(String author)throws SQLException;
    public Collection<Book> findSortedByTitleDesc(String author)throws SQLException;
    public Collection<Book> findSortedByTitleAndPublisher(String author,String publisher)throws SQLException;
}