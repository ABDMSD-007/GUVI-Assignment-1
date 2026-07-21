"""
Case Study 2: Mutual Fund Performance Analytics
ABC Asset Management Company (AMC)

Analyzes fund performance, investor portfolios and investment returns.
Covers: Functions, File Handling, Exception Handling, NumPy, Pandas,
Data Cleaning, GroupBy, Aggregation, Merge, Financial Calculations.
"""

import os
import numpy as np
import pandas as pd

# Directory of this script so the program runs from anywhere
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

RISK_FREE_RATE = 6.0  # % used for Sharpe ratio


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


# ---------------------------------------------------------------------------
# Part 1 - Read Data
# ---------------------------------------------------------------------------
def read_data():
    print("\n" + "=" * 70)
    print("PART 1 - READ DATA")
    print("=" * 70)
    funds = load_csv("funds.csv")
    investors = load_csv("investors.csv")
    transactions = load_csv("transactions.csv")
    nav = load_csv("nav_history.csv")
    return funds, investors, transactions, nav


# ---------------------------------------------------------------------------
# Part 2 - Data Cleaning
# ---------------------------------------------------------------------------
def clean_data(funds, investors, transactions, nav):
    print("\n" + "=" * 70)
    print("PART 2 - DATA CLEANING")
    print("=" * 70)

    # Remove duplicate rows
    before = len(nav)
    funds = funds.drop_duplicates()
    investors = investors.drop_duplicates()
    transactions = transactions.drop_duplicates()
    nav = nav.drop_duplicates()
    print(f"Removed duplicate rows (nav: {before - len(nav)} dropped)")

    # Check missing values
    print("\nMissing values before cleaning:")
    for name, df in [("funds", funds), ("investors", investors),
                     ("transactions", transactions), ("nav", nav)]:
        total = int(df.isnull().sum().sum())
        print(f"  {name}: {total} missing")

    # Strip surrounding whitespace/tabs from all text columns
    for df in (funds, investors, transactions, nav):
        for col in df.select_dtypes(include="object").columns:
            df[col] = df[col].str.strip()

    # Convert Date columns into datetime format
    if "PurchaseDate" in transactions.columns:
        transactions["PurchaseDate"] = pd.to_datetime(
            transactions["PurchaseDate"], errors="coerce")
    if "Date" in nav.columns:
        nav["Date"] = pd.to_datetime(nav["Date"], errors="coerce")

    # Fill missing NAV using Forward Fill (per fund, ordered by date)
    nav = nav.sort_values(["FundID", "Date"])
    nav["NAV"] = nav.groupby("FundID")["NAV"].ffill()
    nav["NAV"] = nav["NAV"].ffill()  # global fallback

    # Replace missing InvestorType with "Retail"
    if "InvestorType" in investors.columns:
        investors["InvestorType"] = investors["InvestorType"].fillna("Retail")

    # Remove rows having negative NAV
    neg = (nav["NAV"] < 0).sum()
    nav = nav[nav["NAV"] >= 0]
    print(f"\nRemoved {neg} rows with negative NAV")

    print("\nMissing values after cleaning:")
    for name, df in [("funds", funds), ("investors", investors),
                     ("transactions", transactions), ("nav", nav)]:
        total = int(df.isnull().sum().sum())
        print(f"  {name}: {total} missing")

    return funds, investors, transactions, nav


# ---------------------------------------------------------------------------
# Part 3 - Merge Data
# ---------------------------------------------------------------------------
def merge_data(funds, investors, transactions, nav):
    print("\n" + "=" * 70)
    print("PART 3 - MERGE DATA")
    print("=" * 70)

    # Latest NAV per fund (most recent date)
    latest_nav = (nav.sort_values("Date")
                     .groupby("FundID")
                     .tail(1)[["FundID", "NAV"]]
                     .rename(columns={"NAV": "LatestNAV"}))

    df = (transactions
          .merge(investors, on="InvestorID", how="left")
          .merge(funds, on="FundID", how="left")
          .merge(latest_nav, on="FundID", how="left"))

    # If a fund has no NAV history, fall back to its purchase NAV
    df["LatestNAV"] = df["LatestNAV"].fillna(df["PurchaseNAV"])

    merged = pd.DataFrame({
        "Investor Name": df["InvestorName"],
        "Fund Name": df["FundName"],
        "Category": df["Category"],
        "AMC": df["AMC"],
        "State": df["State"],
        "InvestorType": df["InvestorType"],
        "Units Purchased": df["UnitsPurchased"],
        "Purchase NAV": df["PurchaseNAV"],
        "Latest NAV": df["LatestNAV"],
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

    merged["Investment Amount"] = merged["Units Purchased"] * merged["Purchase NAV"]
    merged["Current Value"] = merged["Units Purchased"] * merged["Latest NAV"]
    merged["Profit"] = merged["Current Value"] - merged["Investment Amount"]
    merged["ROI %"] = (merged["Profit"] / merged["Investment Amount"]) * 100

    print(merged[["Investor Name", "Fund Name", "Investment Amount",
                  "Current Value", "Profit", "ROI %"]].head())
    return merged


# ---------------------------------------------------------------------------
# Part 5 - NumPy Tasks
# ---------------------------------------------------------------------------
def numpy_tasks(nav):
    print("\n" + "=" * 70)
    print("PART 5 - NUMPY TASKS (on NAV history)")
    print("=" * 70)

    nav_values = nav["NAV"].to_numpy(dtype=float)

    print(f"Average NAV        : {np.mean(nav_values):.4f}")
    print(f"Maximum NAV        : {np.max(nav_values):.4f}")
    print(f"Minimum NAV        : {np.min(nav_values):.4f}")
    print(f"Variance of NAV    : {np.var(nav_values):.4f}")
    print(f"Std Deviation NAV  : {np.std(nav_values):.4f}")

    # Rolling Average (window = 5)
    window = 5
    if len(nav_values) >= window:
        rolling = np.convolve(nav_values, np.ones(window) / window, mode="valid")
        print(f"Rolling Average (window=5), first 5 values: "
              f"{np.round(rolling[:5], 4)}")
    else:
        print("Not enough data for a rolling average of window 5")

    return nav_values


# ---------------------------------------------------------------------------
# Part 6 - Pandas Analysis
# ---------------------------------------------------------------------------
def pandas_analysis(merged, nav):
    print("\n" + "=" * 70)
    print("PART 6 - PANDAS ANALYSIS")
    print("=" * 70)

    print("\nTop 5 investors by investment amount:")
    top_investors = (merged.groupby("Investor Name")["Investment Amount"]
                           .sum().sort_values(ascending=False).head(5))
    print(top_investors)

    print("\nTop 5 profitable funds:")
    top_funds = (merged.groupby("Fund Name")["Profit"]
                       .sum().sort_values(ascending=False).head(5))
    print(top_funds)

    print("\nWorst performing fund (lowest total profit):")
    worst = (merged.groupby("Fund Name")["Profit"].sum().sort_values())
    print(worst.head(1))

    # Highest / Lowest NAV fund from NAV history
    latest = nav.sort_values("Date").groupby("FundID").tail(1)
    highest_nav = latest.loc[latest["NAV"].idxmax()]
    lowest_nav = latest.loc[latest["NAV"].idxmin()]
    print(f"\nHighest NAV fund: {highest_nav['FundID']} "
          f"(NAV = {highest_nav['NAV']})")
    print(f"Lowest NAV fund : {lowest_nav['FundID']} "
          f"(NAV = {lowest_nav['NAV']})")

    return top_investors, top_funds


# ---------------------------------------------------------------------------
# Part 7 - GroupBy
# ---------------------------------------------------------------------------
def groupby_analysis(merged, funds, nav):
    print("\n" + "=" * 70)
    print("PART 7 - GROUPBY")
    print("=" * 70)

    print("\nGroup by Category:")
    category_summary = merged.groupby("Category").agg(
        Average_ROI=("ROI %", "mean"),
        Average_NAV=("Latest NAV", "mean"),
        Total_Investment=("Investment Amount", "sum"),
    ).round(2)
    print(category_summary)

    print("\nGroup by AMC:")
    amc_summary = merged.groupby("AMC").agg(
        Number_of_Funds=("Fund Name", "nunique"),
        Average_NAV=("Latest NAV", "mean"),
        Total_Investment=("Investment Amount", "sum"),
    ).round(2)
    print(amc_summary)

    print("\nGroup by State:")
    state_summary = merged.groupby("State").agg(
        Number_of_Investors=("Investor Name", "nunique"),
        Total_Investment=("Investment Amount", "sum"),
        Average_ROI=("ROI %", "mean"),
    ).round(2)
    print(state_summary)

    print("\nGroup by Investor Type:")
    type_summary = merged.groupby("InvestorType").agg(
        Total_Investment=("Investment Amount", "sum"),
        Average_Profit=("Profit", "mean"),
    ).round(2)
    print(type_summary)

    return category_summary, amc_summary, state_summary, type_summary


# ---------------------------------------------------------------------------
# Part 8 - Detect Issues
# ---------------------------------------------------------------------------
def detect_issues(funds, investors, transactions, nav):
    print("\n" + "=" * 70)
    print("PART 8 - DETECT ISSUES")
    print("=" * 70)

    dup_nav = nav.duplicated(subset=["FundID", "Date"]).sum()
    print(f"Duplicate NAV records : {dup_nav}")

    neg_nav = (nav["NAV"] < 0).sum()
    print(f"Negative NAV records  : {neg_nav}")

    today = pd.Timestamp.now().normalize()
    future_nav = (nav["Date"] > today).sum()
    future_txn = (transactions["PurchaseDate"] > today).sum()
    print(f"Future dates (NAV / transactions): {future_nav} / {future_txn}")

    missing_fund_ids = transactions["FundID"].isnull().sum()
    missing_inv_ids = transactions["InvestorID"].isnull().sum()
    print(f"Missing Fund IDs      : {missing_fund_ids}")
    print(f"Missing Investor IDs  : {missing_inv_ids}")

    invalid_nav = (transactions["PurchaseNAV"] < 0).sum()
    print(f"Invalid Purchase NAV (<0): {invalid_nav}")

    # Referential integrity checks
    orphan_funds = (~transactions["FundID"].isin(funds["FundID"])).sum()
    orphan_investors = (~transactions["InvestorID"].isin(investors["InvestorID"])).sum()
    print(f"Transactions with unknown FundID    : {orphan_funds}")
    print(f"Transactions with unknown InvestorID: {orphan_investors}")


# ---------------------------------------------------------------------------
# Part 9 - Finance Metrics
# ---------------------------------------------------------------------------
def finance_metrics(merged, nav):
    print("\n" + "=" * 70)
    print("PART 9 - FINANCE METRICS")
    print("=" * 70)

    total_invest = merged["Investment Amount"].sum()
    total_current = merged["Current Value"].sum()

    roi = (total_current - total_invest) / total_invest * 100
    absolute_return = total_current - total_invest
    annual_return = roi  # holding period assumed 1 year

    volatility = float(np.std(nav["NAV"].to_numpy(dtype=float)))
    sharpe = (annual_return - RISK_FREE_RATE) / volatility if volatility else np.nan

    print(f"Portfolio ROI %      : {roi:.2f}")
    print(f"Absolute Return      : {absolute_return:.2f}")
    print(f"Annual Return %      : {annual_return:.2f}  (holding period = 1 year)")
    print(f"Volatility (std NAV) : {volatility:.4f}")
    print(f"Risk Free Rate %     : {RISK_FREE_RATE}")
    print(f"Sharpe Ratio         : {sharpe:.4f}")


# ---------------------------------------------------------------------------
# Part 10 - Export Reports
# ---------------------------------------------------------------------------
def export_reports(merged, top_funds, top_investors, category_summary):
    print("\n" + "=" * 70)
    print("PART 10 - EXPORT REPORTS")
    print("=" * 70)

    # TopFunds.xlsx
    top_funds_df = (merged.groupby("Fund Name")
                          .agg(Total_Investment=("Investment Amount", "sum"),
                               Total_Profit=("Profit", "sum"),
                               Average_ROI=("ROI %", "mean"),
                               Latest_NAV=("Latest NAV", "max"))
                          .sort_values("Total_Profit", ascending=False)
                          .round(2))

    # InvestorSummary.xlsx
    investor_summary = (merged.groupby("Investor Name")
                              .agg(Total_Investment=("Investment Amount", "sum"),
                                   Current_Value=("Current Value", "sum"),
                                   Total_Profit=("Profit", "sum"),
                                   Average_ROI=("ROI %", "mean"))
                              .sort_values("Total_Investment", ascending=False)
                              .round(2))

    def out(name):
        return os.path.join(BASE_DIR, name)

    try:
        top_funds_df.to_excel(out("TopFunds.xlsx"))
        investor_summary.to_excel(out("InvestorSummary.xlsx"))
        print("Wrote TopFunds.xlsx and InvestorSummary.xlsx")
    except ModuleNotFoundError:
        # openpyxl not installed - fall back to CSV so the program still runs
        top_funds_df.to_csv(out("TopFunds.csv"))
        investor_summary.to_csv(out("InvestorSummary.csv"))
        print("openpyxl not available -> wrote TopFunds.csv and "
              "InvestorSummary.csv instead")

    category_summary.to_csv(out("CategorySummary.csv"))
    print("Wrote CategorySummary.csv")


# ---------------------------------------------------------------------------
# Expected Outputs summary
# ---------------------------------------------------------------------------
def expected_outputs(merged, nav):
    print("\n" + "=" * 70)
    print("EXPECTED OUTPUTS")
    print("=" * 70)

    fund_stats = merged.groupby("Fund Name").agg(
        Total_Profit=("Profit", "sum"),
        Average_ROI=("ROI %", "mean"),
        Latest_NAV=("Latest NAV", "max"),
    )

    print("\nTop Performing Funds:")
    print(f"  Highest ROI   : {fund_stats['Average_ROI'].idxmax()} "
          f"({fund_stats['Average_ROI'].max():.2f}%)")
    print(f"  Highest Profit: {fund_stats['Total_Profit'].idxmax()} "
          f"({fund_stats['Total_Profit'].max():.2f})")
    print(f"  Highest NAV   : {fund_stats['Latest_NAV'].idxmax()} "
          f"({fund_stats['Latest_NAV'].max():.2f})")

    print("\nWorst Performing Fund:")
    print(f"  Lowest ROI    : {fund_stats['Average_ROI'].idxmin()} "
          f"({fund_stats['Average_ROI'].min():.2f}%)")

    print("\nState-wise Investment:")
    print(merged.groupby("State")["Investment Amount"].sum()
                .sort_values(ascending=False).round(2))

    print("\nAMC-wise Investment:")
    print(merged.groupby("AMC")["Investment Amount"].sum()
                .sort_values(ascending=False).round(2))

    print("\nCategory-wise ROI:")
    print(merged.groupby("Category")["ROI %"].mean().round(2))


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    print("MUTUAL FUND PERFORMANCE ANALYTICS - ABC AMC")

    funds, investors, transactions, nav = read_data()
    funds, investors, transactions, nav = clean_data(
        funds, investors, transactions, nav)

    merged = merge_data(funds, investors, transactions, nav)
    merged = add_metrics(merged)

    numpy_tasks(nav)
    top_investors, top_funds = pandas_analysis(merged, nav)
    category_summary, amc_summary, state_summary, type_summary = \
        groupby_analysis(merged, funds, nav)
    detect_issues(funds, investors, transactions, nav)
    finance_metrics(merged, nav)
    export_reports(merged, top_funds, top_investors, category_summary)
    expected_outputs(merged, nav)

    print("\n" + "=" * 70)
    print("ANALYSIS COMPLETE")
    print("=" * 70)


if __name__ == "__main__":
    main()
