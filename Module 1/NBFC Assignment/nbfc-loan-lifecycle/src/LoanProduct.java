// ============================================================
//  LoanProduct.java  —  Abstract class (Abstraction + Polymorphism)
//
//  OOP Concepts:
//    Abstraction  — abstract class with abstract methods
//    Encapsulation — private fields
//    Polymorphism — subclasses override abstract methods differently
//
//  Subclasses: PersonalLoan, HomeLoan, BusinessLoan
// ============================================================

public abstract class LoanProduct {

    // Encapsulation: private fields
    private String productName;
    private double interestRate; // annual interest rate in %
    private int maxTenureMonths;

    // Constructor called by subclasses using super()
    public LoanProduct(String productName, double interestRate, int maxTenureMonths) {
        this.productName = productName;
        this.interestRate = interestRate;
        this.maxTenureMonths = maxTenureMonths;
    }

    // Abstraction: subclasses MUST define their own loan limits
    public abstract double getMaxLoanAmount();

    // Abstraction: each loan type checks eligibility differently (Polymorphism)
    public abstract boolean checkEligibility(Customer customer, double loanAmount);

    // Concrete method shared by all loan products
    // EMI Formula: P * r * (1+r)^n / ((1+r)^n - 1)
    public double calculateEMI(double principal, int tenureMonths) {
        double monthlyRate = interestRate / (12.0 * 100.0);
        if (monthlyRate == 0)
            return principal / tenureMonths;
        double power = Math.pow(1 + monthlyRate, tenureMonths);
        return (principal * monthlyRate * power) / (power - 1);
    }

    // Shared display method
    public void displayProductInfo() {
        System.out.println("Loan Type     : " + productName);
        System.out.println("Interest Rate : " + interestRate + "% per annum");
        System.out.println("Max Tenure    : " + maxTenureMonths + " months");
        System.out.println("Max Amount    : Rs. " + (int) getMaxLoanAmount());
    }

    // Getters
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
