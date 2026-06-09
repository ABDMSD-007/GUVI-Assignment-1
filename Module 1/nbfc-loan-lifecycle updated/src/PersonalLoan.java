public class PersonalLoan extends LoanProduct {

    public PersonalLoan() {
        super("Personal Loan", 12.5, 60);  // 12.5% interest, max 60 months
    }

    @Override
    public double getMaxLoanAmount() {
        return 1000000.0;
    }

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
