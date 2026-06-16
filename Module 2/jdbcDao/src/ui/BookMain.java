package ui;

import dao.BookDao;
import dao.BookDaoImpl;
import entity.Book;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Scanner;

public class BookMain {

    private static BookDao bookDao = new BookDaoImpl();
    private static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws SQLException {

        while (true) {

            System.out.println("\n===== BOOK MANAGEMENT =====");
            System.out.println("1. Add Book");
            System.out.println("2. Find Book By Id");
            System.out.println("3. View All Books");
            System.out.println("4. Delete Book By Id");
            System.out.println("5. Exit");

            System.out.print("Enter choice: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:
                    saveBook();
                    break;

                case 2:
                    findBookById();
                    break;

                case 3:
                    displayAllBooks();
                    break;

                case 4:
                    deleteBookById();
                    break;

                case 5:
                    System.out.println("Exiting...");
                    return;

                default:
                    System.out.println("Invalid Choice");
            }
        }
    }

    private static void saveBook() throws SQLException {

        System.out.print("Enter Title: ");
        String title = sc.nextLine();

        System.out.print("Enter Author: ");
        String author = sc.nextLine();

        System.out.print("Enter Publisher: ");
        String publisher = sc.nextLine();

        Book book = new Book(title, author, publisher);

        int rows = bookDao.save(book);

        System.out.println(rows + " row inserted.");
    }

    private static void findBookById() throws SQLException {

        System.out.print("Enter Book Id: ");
        int id = sc.nextInt();

        Book book = bookDao.findById(id);

        if (book != null)
            System.out.println(book);
        else
            System.out.println("Book not found");
    }

    private static void displayAllBooks() throws SQLException {

        Collection<Book> books = bookDao.findAll();

        for (Book book : books) {
            System.out.println(book);
        }
    }

    private static void deleteBookById() throws SQLException {

        System.out.print("Enter Book Id: ");
        int id = sc.nextInt();

        bookDao.deleteById(id);

        System.out.println("Book deleted successfully");
    }
}