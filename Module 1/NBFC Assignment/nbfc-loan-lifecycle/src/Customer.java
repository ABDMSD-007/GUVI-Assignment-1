//    Inheritance  — extends Person, reuses name/age
//    Polymorphism — overrides displayInfo()

public class Customer extends Person {

    private String customerId;
    private double monthlyIncome;
    private int creditScore;
    private String city;

    public Customer(String customerId, String name, int age,
            double monthlyIncome,
            int creditScore, String city) {

        super(name, age);
        this.customerId = customerId;
        this.monthlyIncome = monthlyIncome;
        this.creditScore = creditScore;
        this.city = city;
    }

    @Override
    public void displayInfo() {
        System.out.println("Customer Profile:");
        displayBasicInfo();
        System.out.println("  ID     : " + customerId);
        System.out.println("  Income : Rs. " + (int) monthlyIncome + "/month");
        System.out.println("  Credit : " + creditScore + " / 900");
        System.out.println("  City   : " + city);
    }

    public String getCustomerId() {
        return customerId;
    }

    public double getMonthlyIncome() {
        return monthlyIncome;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public String getCity() {
        return city;
    }

    public void setCreditScore(int creditScore) {
        if (creditScore >= 300 && creditScore <= 900) {
            this.creditScore = creditScore;
        } else {
            System.out.println("[ERROR] Credit score must be 300–900.");
        }
    }
}
