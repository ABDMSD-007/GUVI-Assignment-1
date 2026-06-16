package entity;

public class Loan {

    private int id;
    private int amount;
    private int interest;
    private int duration;
    private String loanType;
    private String loanStatus;

    public Loan(int id, int amount, int interest,
                int duration,
                String loanType,
                String loanStatus) {

        this.id = id;
        this.amount = amount;
        this.interest = interest;
        this.duration = duration;
        this.loanType = loanType;
        this.loanStatus = loanStatus;
    }

    public int getId() {
        return id;
    }

    public int getAmount() {
        return amount;
    }

    public int getInterest() {
        return interest;
    }

    public int getDuration() {
        return duration;
    }

    public String getLoanType() {
        return loanType;
    }

    public String getLoanStatus() {
        return loanStatus;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setInterest(int interest) {
        this.interest = interest;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setLoanType(String loanType) {
        this.loanType = loanType;
    }

    public void setLoanStatus(String loanStatus) {
        this.loanStatus = loanStatus;
    }

    @Override
    public String toString() {
        return "Loan{" +
                "id=" + id +
                ", amount=" + amount +
                ", interest=" + interest +
                ", duration=" + duration +
                ", loanType='" + loanType + '\'' +
                ", loanStatus='" + loanStatus + '\'' +
                '}';
    }
}