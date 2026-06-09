public abstract class LoanProduct {

    private String productName;
    private double interestRate; // annual interest rate in %
    private int maxTenureMonths;

    public LoanProduct(String productName, double interestRate, int maxTenureMonths) {
        this.productName = productName;
        this.interestRate = interestRate;
        this.maxTenureMonths = maxTenureMonths;
    }

    public abstract double getMaxLoanAmount();

    public abstract boolean checkEligibility(Customer customer, double loanAmount);

    // EMI Formula: P * r * (1+r)^n / ((1+r)^n - 1)
    public double calculateEMI(double principal, int tenureMonths) {
        double monthlyRate = interestRate / (12.0 * 100.0);
        if (monthlyRate == 0)
            return principal / tenureMonths;
        double power = Math.pow(1 + monthlyRate, tenureMonths);
        return (principal * monthlyRate * power) / (power - 1);
    }

    public void displayProductInfo() {
        System.out.println("Loan Type: " + productName);
        System.out.println("Interest Rate: " + interestRate + "% per annum");
        System.out.println("Max Tenure: " + maxTenureMonths + " months");
        System.out.println("Max Amount: Rs. " + (int) getMaxLoanAmount());
    }

    public String getProductName() {
        return productName;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public int getMaxTenureMonths() {
        return maxTenureMonths;
    }
}
