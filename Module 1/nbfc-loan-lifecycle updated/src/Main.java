public class Main {

    public static void main(String[] args) {

        System.out.println("Available Loan Products:");

        LoanProduct personal = new PersonalLoan();
        LoanProduct home = new HomeLoan();
        LoanProduct business = new BusinessLoan();

        personal.displayProductInfo();
        home.displayProductInfo();
        business.displayProductInfo();

        System.out.println("Customer Registration:");

        Customer c1 = new Customer("C001", "Arjun Sharma", 32, 85000.0, 760, "Bengaluru");
        Customer c2 = new Customer("C002", "Sachin Nair", 38, 120000.0, 700, "Chennai");
        Customer c3 = new Customer("C003", "Rahul Gupta", 28, 45000.0, 590, "Kolkata");

        Person p1 = c1;
        Person p2 = c2;
        Person p3 = c3;
        p1.displayInfo();
        p2.displayInfo();
        p3.displayInfo();

        System.out.println("Application 1: Arjun - Personal Loan");
        LoanApplication app1 = new LoanApplication("APP-001", c1, personal, 500000.0, 48);
        app1.processLoan();
        app1.displaySummary();

        System.out.println("Application 2: Sachin - Home Loan");
        LoanApplication app2 = new LoanApplication("APP-002", c2, home, 3000000.0, 180);
        app2.processLoan();
        app2.displaySummary();

        System.out.println("Application 3: Rahul - Business Loan");
        LoanApplication app3 = new LoanApplication("APP-003", c3, business, 1000000.0, 60);
        app3.processLoan();
        app3.displaySummary();

        int total = 3;
        for (int i = 1; i <= total; i++) {

            LoanApplication current;
            switch (i) {
                case 1:
                    current = app1;
                    break;
                case 2:
                    current = app2;
                    break;
                case 3:
                    current = app3;
                    break;
                default:
                    current = null;
            }

            if (current != null) {
                System.out.println(current.getApplicationId() + "   " +
                        current.getCustomer().getName() + "   " +
                        current.getStatus());
            }
        }
    }
}
