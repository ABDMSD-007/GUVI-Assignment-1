// LoanApplication.java - Core lifecycle class
// Concepts: Encapsulation, Aggregation, While loop, Switch case

public class LoanApplication {

    // Encapsulation: private fields
    private String      applicationId;
    private String      status;
    private double      loanAmount;
    private int         tenureMonths;

    // Aggregation: Customer and LoanProduct exist independently
    private Customer    customer;
    private LoanProduct loanProduct;

    // EMI stored after approval
    private double      monthlyEMI;

    // Constructor
    public LoanApplication(String applicationId, Customer customer,
                           LoanProduct loanProduct, double loanAmount,
                           int tenureMonths) {
        this.applicationId      = applicationId;
        this.customer           = customer;
        this.loanProduct        = loanProduct;
        this.loanAmount         = loanAmount;
        this.tenureMonths       = tenureMonths;
        this.status             = "PENDING";
        this.monthlyEMI         = 0;
    }

    // Walks through the loan lifecycle using a while loop and switch case
    public void processLoan() {
        System.out.println("Processing: " + applicationId + " [" + loanProduct.getProductName() + "]");

        int step = 1;
        boolean processing = true;   // while loop control flag

        while (processing) {         // while loop - runs until loan is done

            switch (step) {          // switch case - one case per lifecycle stage

                case 1:
                    System.out.println("Step 1: Document Verification - VERIFIED");
                    status = "UNDER_REVIEW";
                    step = 2;
                    break;

                case 2:
                    int score = customer.getCreditScore();
                    String rating;
                    if      (score >= 750) rating = "EXCELLENT";
                    else if (score >= 700) rating = "GOOD";
                    else if (score >= 650) rating = "FAIR";
                    else                   rating = "POOR";
                    System.out.println("Step 2: Credit Score - " + score + " (" + rating + ")");
                    step = 3;
                    break;

                case 3:
                    System.out.println("Step 3: Eligibility Check");
                    boolean eligible = loanProduct.checkEligibility(customer, loanAmount);
                    if (eligible) {
                        step = 4;
                    } else {
                        status = "REJECTED";
                        processing = false;   // stop the while loop
                        System.out.println("Result: APPLICATION REJECTED");
                    }
                    break;

                case 4:
                    System.out.println("Step 4: EMI Calculation");
                    monthlyEMI = loanProduct.calculateEMI(loanAmount, tenureMonths);
                    System.out.println("Interest Rate: " + loanProduct.getInterestRate() + "%");
                    System.out.printf("Monthly EMI: Rs. %.2f%n", monthlyEMI);
                    status = "APPROVED";
                    System.out.println("Result: LOAN APPROVED - Rs. " + (int) loanAmount +
                                       " disbursed to " + customer.getName());
                    processing = false;       // stop the while loop
                    break;

                default:
                    System.out.println("Unknown step.");
                    processing = false;
            }
        } // end while

        System.out.println("Final Status: " + status);
    }

    // Summary printout
    public void displaySummary() {
        System.out.println("Summary for " + applicationId + ":");
        System.out.println("  Customer  : " + customer.getName());
        System.out.println("  Loan Type : " + loanProduct.getProductName());
        System.out.println("  Amount    : Rs. " + (int) loanAmount);
        System.out.println("  Tenure    : " + tenureMonths + " months");
        System.out.println("  Status    : " + status);
        if (monthlyEMI > 0) {
            System.out.printf("  EMI       : Rs. %.2f/month%n", monthlyEMI);
        }
    }

    // Getters
    public String      getApplicationId() { return applicationId; }
    public String      getStatus()        { return status;        }
    public Customer    getCustomer()      { return customer;      }
    public LoanProduct getLoanProduct()   { return loanProduct;   }
}
