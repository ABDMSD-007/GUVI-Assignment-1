// ============================================================
//  HomeLoan.java  —  Extends LoanProduct (Inheritance + Polymorphism)
//
//  OOP Concepts:
//    Inheritance  — extends LoanProduct
//    Polymorphism — overrides getMaxLoanAmount() and checkEligibility()
// ============================================================

public class HomeLoan extends LoanProduct {

    public HomeLoan() {
        super("Home Loan", 8.5, 240);  // 8.5% interest, max 240 months (20 years)
    }

    // Polymorphism: override abstract method from LoanProduct
    @Override
    public double getMaxLoanAmount() {
        return 10000000.0;  // Rs. 1 Crore
    }

    // Polymorphism: home loan has stricter eligibility rules
    @Override
    public boolean checkEligibility(Customer customer, double loanAmount) {
        boolean creditOk  = customer.getCreditScore() >= 700;
        boolean amountOk  = loanAmount <= getMaxLoanAmount();
        boolean incomeOk  = customer.getMonthlyIncome() >= 40000;

        System.out.println("  Credit >= 700 ? " + creditOk);
        System.out.println("  Amount in range? " + amountOk);
        System.out.println("  Income >= 40k ? " + incomeOk);

        return creditOk && amountOk && incomeOk;
    }
}
