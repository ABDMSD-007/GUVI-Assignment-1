// Inheritance  — extends LoanProduct
// Polymorphism — overrides getMaxLoanAmount() and checkEligibility()

public class BusinessLoan extends LoanProduct {

    public BusinessLoan() {
        super("Business Loan", 14.0, 84);  // 14% interest, max 84 months
    }

    @Override
    public double getMaxLoanAmount() {
        return 5000000.0;
    }

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
