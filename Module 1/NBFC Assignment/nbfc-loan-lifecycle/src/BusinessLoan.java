// ============================================================
//  BusinessLoan.java  —  Extends LoanProduct (Inheritance + Polymorphism)
//
//  OOP Concepts:
//    Inheritance  — extends LoanProduct
//    Polymorphism — overrides getMaxLoanAmount() and checkEligibility()
// ============================================================

public class BusinessLoan extends LoanProduct {

    public BusinessLoan() {
        super("Business Loan", 14.0, 84);  // 14% interest, max 84 months (7 years)
    }

    // Polymorphism: override abstract method from LoanProduct
    @Override
    public double getMaxLoanAmount() {
        return 5000000.0;  // Rs. 50 Lakhs
    }

    // Polymorphism: business loan checks eligibility differently
    @Override
    public boolean checkEligibility(Customer customer, double loanAmount) {
        boolean creditOk  = customer.getCreditScore() >= 680;
        boolean amountOk  = loanAmount <= getMaxLoanAmount();
        boolean incomeOk  = customer.getMonthlyIncome() >= 50000;

        System.out.println("  Credit >= 680 ? " + creditOk);
        System.out.println("  Amount in range? " + amountOk);
        System.out.println("  Income >= 50k ? " + incomeOk);

        return creditOk && amountOk && incomeOk;
    }
}
