// ============================================================
//  PersonalLoan.java  —  Extends LoanProduct (Inheritance + Polymorphism)
//
//  OOP Concepts:
//    Inheritance  — extends LoanProduct
//    Polymorphism — overrides getMaxLoanAmount() and checkEligibility()
// ============================================================

public class PersonalLoan extends LoanProduct {

    public PersonalLoan() {
        super("Personal Loan", 12.5, 60);  // 12.5% interest, max 60 months
    }

    // Polymorphism: override abstract method from LoanProduct
    @Override
    public double getMaxLoanAmount() {
        return 1000000.0;  // Rs. 10 Lakhs
    }

    // Polymorphism: personal loan eligibility rules
    @Override
    public boolean checkEligibility(Customer customer, double loanAmount) {
        boolean creditOk  = customer.getCreditScore() >= 650;
        boolean amountOk  = loanAmount <= getMaxLoanAmount();
        boolean incomeOk  = customer.getMonthlyIncome() >= 20000;

        System.out.println("  Credit >= 650 ? " + creditOk);
        System.out.println("  Amount in range? " + amountOk);
        System.out.println("  Income >= 20k ? " + incomeOk);

        return creditOk && amountOk && incomeOk;
    }
}
