package ui;

import dao.LoanDao;
import dao.LoanDaoImpl;
import entity.Loan;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Scanner;

public class MainLoan {

    private static final LoanDao loanDao = new LoanDaoImpl();
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws SQLException {

        while (true) {

            System.out.println("1 Save");
            System.out.println("2 Find");
            System.out.println("3 View");
            System.out.println("4 Find By Type");
            System.out.println("5 Update");
            System.out.println("6 Delete");
            System.out.println("7 Exit");

            int ch = sc.nextInt();

            switch (ch) {

                case 1:
                    saveLoan();
                    break;

                case 2:
                    findLoan();
                    break;

                case 3:
                    viewLoans();
                    break;

                case 4:
                    findByType();
                    break;

                case 5:
                    updateLoan();
                    break;

                case 6:
                    deleteLoan();
                    break;

                case 7:
                    return;

                default:
                    System.out.println("Invalid");
            }
        }
    }

    private static void saveLoan() throws SQLException {

        System.out.println("Id");
        int id = sc.nextInt();

        System.out.println("Amount");
        int amount = sc.nextInt();

        System.out.println("Interest");
        int interest = sc.nextInt();

        System.out.println("Duration");
        int duration = sc.nextInt();

        sc.nextLine();

        System.out.println("Type");
        String type = sc.nextLine();

        System.out.println("Status");
        String status = sc.nextLine();

        Loan loan =
                new Loan(id, amount, interest,
                        duration, type, status);

        System.out.println(loanDao.save(loan));
    }

    private static void findLoan() throws SQLException {

        System.out.println("Id");

        int id = sc.nextInt();

        System.out.println(loanDao.findById(id));
    }

    private static void viewLoans() throws SQLException {

        Collection<Loan> loans =
                loanDao.findAll();

        loans.forEach(System.out::println);
    }

    private static void findByType() throws SQLException {

        sc.nextLine();

        System.out.println("Type");

        String type = sc.nextLine();

        Collection<Loan> loans =
                loanDao.findByType(type);

        loans.forEach(System.out::println);
    }

    private static void updateLoan() throws SQLException {

        System.out.println("Id");
        int id = sc.nextInt();

        System.out.println("Amount");
        int amount = sc.nextInt();

        System.out.println("Interest");
        int interest = sc.nextInt();

        System.out.println("Duration");
        int duration = sc.nextInt();

        sc.nextLine();

        System.out.println("Type");
        String type = sc.nextLine();

        System.out.println("Status");
        String status = sc.nextLine();

        Loan loan =
                new Loan(id, amount, interest,
                        duration, type, status);

        loanDao.updateById(id, loan);

        System.out.println("Updated");
    }

    private static void deleteLoan() throws SQLException {

        System.out.println("Id");

        int id = sc.nextInt();

        loanDao.deleteById(id);

        System.out.println("Deleted");
    }
}