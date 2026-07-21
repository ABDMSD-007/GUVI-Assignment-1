"""
Case Study 3: Loan Processing & Loan Repayment Analytics
ABC Bank - Loan Processing Department

Automates loan-approval insights and analyzes repayment performance.
Skills: Functions, File Handling, Exception Handling, NumPy, Pandas,
Data Cleaning, GroupBy, Aggregation, Merge, Financial Calculations.

NOTE ON SOURCE DATA (reconciled in code):
  * customers.csv CustomerID = 'C101'  but loan_application CustomerID = 101
    -> merged on a normalized numeric customer key.
  * loan_application LoanID = 'L1001..L1025' while loan_payments LoanID =
    'L101..L125' -> merged on the numeric loan sequence (last two digits).
  * There is NO Credit Score column in the data -> a deterministic (seeded)
    Credit Score is generated so the credit-score analyses can run.
  * loan_payments has EMIAmount / PaidEMIs / PendingEMIs (no 'Amount Paid'):
      Amount Paid   = EMIAmount * PaidEMIs
      Total Payable = EMIAmount * (PaidEMIs + PendingEMIs)
      EMI Due       = Total Payable - Amount Paid
      Payment Status derived from PendingEMIs / PaidEMIs.
"""

import os
import re
import numpy as np
import pandas as pd

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CREDIT_SCORE_MISSING_THRESHOLD = 650
DTI_FLAG = 5
EMI_DUE_FLAG = 10000
HIGH_LOAN_FLAG = 3000000      # 30 Lakhs
LOW_SALARY_FLAG = 30000
LARGE_LOAN = 2000000          # 20 Lakhs


def load_csv(filename):
    """Read a CSV file with exception handling."""
    path = os.path.join(BASE_DIR, filename)
    try:
        df = pd.read_csv(path)
        print(f"Loaded {filename}: {df.shape[0]} rows, {df.shape[1]} columns")
        return df
    except FileNotFoundError:
        print(f"ERROR: {filename} not found at {path}")
        raise
    except pd.errors.EmptyDataError:
        print(f"ERROR: {filename} is empty")
        raise


def numeric_key(value):
    """Return the integer digits contained in an id string (or NaN)."""
    if pd.isna(value):
        return np.nan
    digits = re.sub(r"\D", "", str(value))
    return int(digits) if digits else np.nan


# ---------------------------------------------------------------------------
# Part 1 - Read Data
# ---------------------------------------------------------------------------
def read_data():
    print("\n" + "=" * 70)
    print("PART 1 - READ DATA")
    print("=" * 70)
    customers = load_csv("customers.csv")
    applications = load_csv("loan_application.csv")
    payments = load_csv("loan_payments.csv")
    return customers, applications, payments


# ---------------------------------------------------------------------------
# Part 2 - Data Cleaning
# ---------------------------------------------------------------------------
def clean_data(customers, applications, payments):
    print("\n" + "=" * 70)
    print("PART 2 - DATA CLEANING")
    print("=" * 70)

    # Remove duplicate records
    customers = customers.drop_duplicates()
    applications = applications.drop_duplicates()
    payments = payments.drop_duplicates()

    # Remove duplicate Loan IDs
    applications = applications.drop_duplicates(subset=["LoanID"])
    payments = payments.drop_duplicates(subset=["LoanID"])
    print("Removed duplicate records and duplicate Loan IDs")

    # Strip whitespace from text columns
    for df in (customers, applications, payments):
        text_cols = [c for c in df.columns if df[c].dtype == object]
        for col in text_cols:
            df[col] = df[col].str.strip()

    # Check missing values
    print("\nMissing values before cleaning:")
    for name, df in [("customers", customers), ("applications", applications),
                     ("payments", payments)]:
        print(f"  {name}: {int(df.isnull().sum().sum())} missing")

    # Replace missing Salary with Median Salary
    if "Salary" in customers.columns:
        median_salary = customers["Salary"].median()
        customers["Salary"] = customers["Salary"].fillna(median_salary)

    # Synthesize a deterministic Credit Score (absent from source data)
    if "CreditScore" not in customers.columns:
        rng = np.random.default_rng(42)
        salary = customers["Salary"].fillna(customers["Salary"].median())
        norm = (salary - salary.min()) / (salary.max() - salary.min() + 1e-9)
        base = 580 + norm * 200            # ~580..780 driven by salary
        noise = rng.integers(-40, 40, size=len(customers))
        customers["CreditScore"] = np.clip(base + noise, 300, 900).round().astype(int)
        print("\nNOTE: 'CreditScore' not present in data -> generated "
              "deterministically (seed=42).")

    # Replace missing Credit Score with Mean Credit Score
    mean_credit = customers["CreditScore"].mean()
    customers["CreditScore"] = customers["CreditScore"].fillna(mean_credit)

    # Convert ApplicationDate and PaymentDate to datetime
    if "ApplicationDate" in applications.columns:
        applications["ApplicationDate"] = pd.to_datetime(
            applications["ApplicationDate"], errors="coerce")
    if "LastPaymentDate" in payments.columns:
        payments["LastPaymentDate"] = pd.to_datetime(
            payments["LastPaymentDate"], errors="coerce")

    # Remove negative Loan Amounts
    neg_loans = (applications["LoanAmount"] < 0).sum()
    applications = applications[applications["LoanAmount"] >= 0]
    print(f"\nRemoved {neg_loans} negative loan amounts")

    # Remove invalid EMI Amounts (<= 0 or missing)
    invalid_emi = ((payments["EMIAmount"] <= 0) | payments["EMIAmount"].isnull()).sum()
    payments = payments[payments["EMIAmount"] > 0]
    print(f"Removed {invalid_emi} invalid EMI amounts")

    # Remove future payment dates
    today = pd.Timestamp.now().normalize()
    future = (payments["LastPaymentDate"] > today).sum()
    payments = payments[(payments["LastPaymentDate"].isna()) |
                        (payments["LastPaymentDate"] <= today)]
    print(f"Removed {future} future payment dates")

    return customers, applications, payments


# ---------------------------------------------------------------------------
# Part 3 - Merge Datasets
# ---------------------------------------------------------------------------
def merge_data(customers, applications, payments):
    print("\n" + "=" * 70)
    print("PART 3 - MERGE DATASETS")
    print("=" * 70)

    # Normalize join keys across the inconsistent id formats
    customers = customers.copy()
    applications = applications.copy()
    payments = payments.copy()

    customers["CustKey"] = customers["CustomerID"].apply(numeric_key)
    applications["CustKey"] = applications["CustomerID"].apply(numeric_key)

    # Loan sequence: L1001->1, L101->1 (last two digits of the numeric part)
    applications["LoanKey"] = applications["LoanID"].apply(
        lambda x: numeric_key(x) % 100)
    payments["LoanKey"] = payments["LoanID"].apply(
        lambda x: numeric_key(x) % 100)

    df = (applications
          .merge(customers, on="CustKey", how="left",
                 suffixes=("", "_cust"))
          .merge(payments, on="LoanKey", how="left",
                 suffixes=("", "_pay")))

    # Derived monetary fields from EMI counts
    df["Amount Paid"] = df["EMIAmount"] * df["PaidEMIs"]
    df["Total Payable"] = df["EMIAmount"] * (df["PaidEMIs"] + df["PendingEMIs"])

    # Derived Payment Status
    def payment_status(row):
        if pd.isna(row["PendingEMIs"]):
            return "Unknown"
        if row["PendingEMIs"] == 0:
            return "Paid"
        if row["PaidEMIs"] == 0:
            return "Pending"
        return "Partial"

    df["Payment Status"] = df.apply(payment_status, axis=1)

    merged = pd.DataFrame({
        "LoanID": df["LoanID"],
        "Customer Name": df["CustomerName"],
        "City": df["City"],
        "State": df["State"],
        "BranchID": df["BranchID"],
        "Loan Type": df["LoanType"],
        "Loan Amount": df["LoanAmount"],
        "Credit Score": df["CreditScore"],
        "Salary": df["Salary"],
        "Loan Status": df["LoanStatus"],
        "EMI Amount": df["EMIAmount"],
        "Amount Paid": df["Amount Paid"],
        "Total Payable": df["Total Payable"],
        "Payment Status": df["Payment Status"],
    })
    print(f"Merged dataset: {merged.shape[0]} rows, {merged.shape[1]} columns")
    print(merged.head())
    return merged


# ---------------------------------------------------------------------------
# Part 4 - Create New Columns
# ---------------------------------------------------------------------------
def add_metrics(merged):
    print("\n" + "=" * 70)
    print("PART 4 - CREATE NEW COLUMNS")
    print("=" * 70)

    merged["Monthly Income"] = merged["Salary"] / 12
    merged["Debt-to-Income Ratio"] = merged["Loan Amount"] / merged["Salary"]
    # EMI Due = total payable - amount paid (outstanding repayment)
    merged["EMI Due"] = merged["Total Payable"] - merged["Amount Paid"]
    merged["Payment Completion %"] = np.where(
        merged["Total Payable"] > 0,
        merged["Amount Paid"] / merged["Total Payable"] * 100,
        0)

    print(merged[["Customer Name", "Monthly Income", "Debt-to-Income Ratio",
                  "EMI Due", "Payment Completion %"]].head())
    return merged


# ---------------------------------------------------------------------------
# Part 5 - NumPy Tasks
# ---------------------------------------------------------------------------
def numpy_tasks(merged):
    print("\n" + "=" * 70)
    print("PART 5 - NUMPY TASKS (Loan Amount)")
    print("=" * 70)

    amounts = merged["Loan Amount"].dropna().to_numpy(dtype=float)
    print(f"Average Loan Amount   : {np.mean(amounts):,.2f}")
    print(f"Median Loan Amount    : {np.median(amounts):,.2f}")
    print(f"Maximum Loan Amount   : {np.max(amounts):,.2f}")
    print(f"Minimum Loan Amount   : {np.min(amounts):,.2f}")
    print(f"Standard Deviation    : {np.std(amounts):,.2f}")
    print(f"Variance              : {np.var(amounts):,.2f}")
    print(f"25th Percentile       : {np.percentile(amounts, 25):,.2f}")
    print(f"75th Percentile       : {np.percentile(amounts, 75):,.2f}")


# ---------------------------------------------------------------------------
# Part 6 - Pandas Analysis
# ---------------------------------------------------------------------------
def pandas_analysis(merged):
    print("\n" + "=" * 70)
    print("PART 6 - PANDAS ANALYSIS")
    print("=" * 70)

    print("\nTop 10 highest loan customers:")
    print(merged.nlargest(10, "Loan Amount")[
        ["Customer Name", "Loan Type", "Loan Amount"]].to_string(index=False))

    print("\nTop 10 customers by salary:")
    print(merged.nlargest(10, "Salary")[
        ["Customer Name", "Salary"]].to_string(index=False))

    print("\nCustomers with Credit Score below 650:")
    low_credit = merged[merged["Credit Score"] < 650]
    print(low_credit[["Customer Name", "Credit Score"]].to_string(index=False))

    print("\nCustomers with Loan Amount greater than 20 Lakhs:")
    big = merged[merged["Loan Amount"] > LARGE_LOAN]
    print(big[["Customer Name", "Loan Amount"]].to_string(index=False))

    print(f"\nLoans with Pending Payments: "
          f"{(merged['Payment Status'] != 'Paid').sum()}")
    print(f"Fully Paid Loans          : "
          f"{(merged['Payment Status'] == 'Paid').sum()}")


# ---------------------------------------------------------------------------
# Part 7 - GroupBy
# ---------------------------------------------------------------------------
def groupby_analysis(merged):
    print("\n" + "=" * 70)
    print("PART 7 - GROUPBY")
    print("=" * 70)

    print("\nGroup by City:")
    city = merged.groupby("City").agg(
        Number_of_Customers=("Customer Name", "nunique"),
        Average_Salary=("Salary", "mean"),
        Total_Loan_Amount=("Loan Amount", "sum"),
    ).round(2)
    print(city)

    print("\nGroup by Loan Type:")
    loan_type = merged.groupby("Loan Type").agg(
        Number_of_Loans=("LoanID", "count"),
        Average_Loan_Amount=("Loan Amount", "mean"),
        Total_Loan_Amount=("Loan Amount", "sum"),
    ).round(2)
    print(loan_type)

    print("\nGroup by Loan Status (Approved / Pending / Rejected):")
    print(merged["Loan Status"].value_counts())

    print("\nGroup by Payment Status (count & total amount paid):")
    pay = merged.groupby("Payment Status").agg(
        Count=("LoanID", "count"),
        Total_Amount_Paid=("Amount Paid", "sum"),
    ).round(2)
    print(pay)

    return city, loan_type


# ---------------------------------------------------------------------------
# Part 8 - Business Rules
# ---------------------------------------------------------------------------
def business_rules(merged):
    print("\n" + "=" * 70)
    print("PART 8 - BUSINESS RULES (flagged loans)")
    print("=" * 70)

    merged["Flag_HighLoan"] = merged["Loan Amount"] > HIGH_LOAN_FLAG
    merged["Flag_LowCredit"] = merged["Credit Score"] < 650
    merged["Flag_LowSalary"] = merged["Salary"] < LOW_SALARY_FLAG
    merged["Flag_HighDTI"] = merged["Debt-to-Income Ratio"] > DTI_FLAG
    merged["Flag_HighEMIDue"] = merged["EMI Due"] > EMI_DUE_FLAG
    merged["Flag_PendingPayment"] = merged["Payment Status"] == "Pending"
    merged["Flag_Rejected"] = merged["Loan Status"] == "Rejected"

    flag_cols = [c for c in merged.columns if c.startswith("Flag_")]
    print("Flag counts:")
    for col in flag_cols:
        print(f"  {col.replace('Flag_', ''):<16}: {int(merged[col].sum())}")

    merged["Total Flags"] = merged[flag_cols].sum(axis=1)
    print("\nHighest-risk loans (most flags):")
    print(merged.nlargest(5, "Total Flags")[
        ["Customer Name", "Loan Type", "Loan Amount", "Total Flags"]
    ].to_string(index=False))
    return merged


# ---------------------------------------------------------------------------
# Part 9 - Finance Metrics
# ---------------------------------------------------------------------------
def finance_metrics(merged):
    print("\n" + "=" * 70)
    print("PART 9 - FINANCE METRICS")
    print("=" * 70)

    total_portfolio = merged["Loan Amount"].sum()
    total_collected = merged["Amount Paid"].sum()
    outstanding = merged["Loan Amount"].sum() - total_collected
    recovery_pct = total_collected / total_portfolio * 100 if total_portfolio else 0

    total_loans = len(merged)
    pending_loans = (merged["Loan Status"] == "Pending").sum()
    default_pct = pending_loans / total_loans * 100 if total_loans else 0

    print(f"Total Loan Portfolio  : {total_portfolio:,.2f}")
    print(f"Total Amount Collected: {total_collected:,.2f}")
    print(f"Outstanding Amount    : {outstanding:,.2f}")
    print(f"Loan Recovery %       : {recovery_pct:.2f}")
    print(f"Default % (pending)   : {default_pct:.2f}")
    print(f"Average EMI           : {merged['EMI Amount'].mean():,.2f}")
    print(f"Average Credit Score  : {merged['Credit Score'].mean():.2f}")


# ---------------------------------------------------------------------------
# Part 10 - Export Reports
# ---------------------------------------------------------------------------
def export_reports(merged, city_summary, loan_type_summary):
    print("\n" + "=" * 70)
    print("PART 10 - EXPORT REPORTS")
    print("=" * 70)

    def out(name):
        return os.path.join(BASE_DIR, name)

    loan_summary = pd.concat([
        loan_type_summary,
    ])

    customer_report = merged[[
        "LoanID", "Customer Name", "City", "Loan Type", "Loan Amount",
        "Credit Score", "Salary", "Loan Status", "EMI Amount", "Amount Paid",
        "EMI Due", "Payment Completion %", "Payment Status",
    ]].round(2)

    pending_payments = merged[merged["Payment Status"] != "Paid"][[
        "LoanID", "Customer Name", "City", "Loan Amount", "EMI Amount",
        "Amount Paid", "EMI Due", "Payment Status",
    ]].round(2)

    try:
        with pd.ExcelWriter(out("LoanSummary.xlsx")) as writer:
            loan_type_summary.to_excel(writer, sheet_name="LoanType")
            city_summary.to_excel(writer, sheet_name="City")
        customer_report.to_excel(out("CustomerLoanReport.xlsx"), index=False)
        print("Wrote LoanSummary.xlsx and CustomerLoanReport.xlsx")
    except ModuleNotFoundError:
        loan_type_summary.to_csv(out("LoanSummary.csv"))
        customer_report.to_csv(out("CustomerLoanReport.csv"), index=False)
        print("openpyxl not available -> wrote LoanSummary.csv and "
              "CustomerLoanReport.csv instead")

    pending_payments.to_csv(out("PendingPayments.csv"), index=False)
    print("Wrote PendingPayments.csv")


# ---------------------------------------------------------------------------
# Expected Outputs summary
# ---------------------------------------------------------------------------
def expected_outputs(merged):
    print("\n" + "=" * 70)
    print("EXPECTED OUTPUTS")
    print("=" * 70)

    print("\nTop 10 Loan Customers:")
    print(merged.nlargest(10, "Loan Amount")[
        ["Customer Name", "Loan Amount"]].to_string(index=False))

    print("\nCustomers with Low Credit Score (<650):")
    print(merged[merged["Credit Score"] < 650][
        ["Customer Name", "Credit Score"]].to_string(index=False))

    print("\nPending Loan Payments:")
    print(merged[merged["Payment Status"] != "Paid"][
        ["Customer Name", "EMI Due", "Payment Status"]].to_string(index=False))

    print("\nCity-wise Loan Summary:")
    print(merged.groupby("City")["Loan Amount"].sum()
                .sort_values(ascending=False).round(2))

    print("\nLoan Type Summary:")
    print(merged.groupby("Loan Type")["Loan Amount"]
                .agg(["count", "sum", "mean"]).round(2))

    print("\nLoan Recovery Report:")
    total = merged["Loan Amount"].sum()
    collected = merged["Amount Paid"].sum()
    print(f"  Total Portfolio : {total:,.2f}")
    print(f"  Collected       : {collected:,.2f}")
    print(f"  Recovery %      : {collected / total * 100:.2f}")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    print("LOAN PROCESSING & REPAYMENT ANALYTICS - ABC BANK")

    customers, applications, payments = read_data()
    customers, applications, payments = clean_data(
        customers, applications, payments)

    merged = merge_data(customers, applications, payments)
    merged = add_metrics(merged)

    numpy_tasks(merged)
    pandas_analysis(merged)
    city_summary, loan_type_summary = groupby_analysis(merged)
    merged = business_rules(merged)
    finance_metrics(merged)
    export_reports(merged, city_summary, loan_type_summary)
    expected_outputs(merged)

    print("\n" + "=" * 70)
    print("ANALYSIS COMPLETE")
    print("=" * 70)


if __name__ == "__main__":
    main()
