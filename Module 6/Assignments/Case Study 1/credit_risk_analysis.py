"""
Case Study 1: Credit Risk & Loan Portfolio Analysis
=====================================================
Reads customer, loan and credit-score data, cleans it, engineers risk and
finance metrics, identifies high-risk customers and produces automated reports.

Outputs
-------
- risk_report.xlsx        : multi-sheet Excel workbook (portfolio + metrics)
- high_risk_customers.csv : top-20 risky customers
- summary.json            : machine-readable summary of key metrics
"""

from __future__ import annotations

import json
import os
from dataclasses import dataclass, field

import numpy as np
import pandas as pd

# --------------------------------------------------------------------------- #
# Configuration
# --------------------------------------------------------------------------- #
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

CUSTOMERS_FILE = os.path.join(BASE_DIR, "customers.csv")
LOANS_FILE = os.path.join(BASE_DIR, "loans.csv")
CREDIT_FILE = os.path.join(BASE_DIR, "credit_scores.csv")

XLSX_REPORT = os.path.join(BASE_DIR, "risk_report.xlsx")
HIGH_RISK_CSV = os.path.join(BASE_DIR, "high_risk_customers.csv")
SUMMARY_JSON = os.path.join(BASE_DIR, "summary.json")

# Risk thresholds (from the case-study brief)
CREDIT_SCORE_THRESHOLD = 650
SALARY_THRESHOLD = 60_000
LOAN_THRESHOLD = 1_000_000          # 10 Lakhs
TOP_N_RISKY = 20

# Loss modelling assumption (Loss Given Default)
LGD = 0.45


# --------------------------------------------------------------------------- #
# OOP: Loan class
# --------------------------------------------------------------------------- #
@dataclass
class Loan:
    """Represents a single loan and encapsulates its risk/finance behaviour."""

    loan_id: str
    customer_id: int
    loan_amount: float
    interest_rate: float
    tenure: int
    emi: float
    paid_emis: int
    default_flag: int
    salary: float = np.nan
    credit_score: float = np.nan

    outstanding: float = field(init=False)

    def __post_init__(self) -> None:
        self.outstanding = self.emi * max(self.tenure - self.paid_emis, 0)

    def debt_to_income(self) -> float:
        """Monthly EMI as a fraction of monthly income."""
        monthly_income = self.salary / 12.0
        if not monthly_income or np.isnan(monthly_income):
            return np.nan
        return self.emi / monthly_income

    def loan_utilization(self) -> float:
        """Fraction of the loan still outstanding."""
        if not self.loan_amount:
            return np.nan
        return self.outstanding / self.loan_amount

    def expected_loss(self, lgd: float = LGD) -> float:
        """Expected Loss = PD * LGD * EAD (PD proxied by the default flag)."""
        pd_ = float(self.default_flag)
        return pd_ * lgd * self.outstanding

    def is_high_risk(self) -> bool:
        return (
            (self.credit_score < CREDIT_SCORE_THRESHOLD)
            and (self.salary < SALARY_THRESHOLD)
            and (self.loan_amount > LOAN_THRESHOLD)
            and (self.default_flag == 1)
        )


# --------------------------------------------------------------------------- #
# Data loading with exception handling
# --------------------------------------------------------------------------- #
def read_csv_safe(path: str, required_cols: list[str]) -> pd.DataFrame:
    """Read a CSV robustly, tolerating corrupted/malformed rows.

    Raises FileNotFoundError / ValueError with a clear message on failure.
    """
    if not os.path.exists(path):
        raise FileNotFoundError(f"Input file not found: {path}")

    try:
        # on_bad_lines='skip' drops corrupted rows instead of crashing.
        df = pd.read_csv(path, on_bad_lines="skip", skip_blank_lines=True)
    except pd.errors.EmptyDataError as exc:
        raise ValueError(f"File is empty: {path}") from exc
    except pd.errors.ParserError as exc:
        raise ValueError(f"File is corrupted and cannot be parsed: {path}") from exc
    except UnicodeDecodeError:
        # Fallback for files saved with a different encoding.
        df = pd.read_csv(path, on_bad_lines="skip", encoding="latin-1")

    missing = [c for c in required_cols if c not in df.columns]
    if missing:
        raise ValueError(f"{os.path.basename(path)} is missing columns: {missing}")

    return df


def load_data() -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    """Load the three source files."""
    customers = read_csv_safe(CUSTOMERS_FILE, ["CustomerID", "Age", "Salary", "City"])
    loans = read_csv_safe(
        LOANS_FILE,
        ["LoanID", "CustomerID", "LoanAmount", "InterestRate",
         "Tenure", "EMI", "PaidEMIs", "DefaultFlag"],
    )
    credit = read_csv_safe(CREDIT_FILE, ["CustomerID", "CreditScore"])
    return customers, loans, credit


# --------------------------------------------------------------------------- #
# NumPy statistics
# --------------------------------------------------------------------------- #
def numpy_statistics(df: pd.DataFrame) -> dict:
    """Compute the required portfolio statistics using NumPy."""
    loan_amount = df["LoanAmount"].to_numpy(dtype=float)
    salary = df["Salary"].to_numpy(dtype=float)
    interest = df["InterestRate"].to_numpy(dtype=float)

    stats = {
        "mean_loan_amount": float(np.mean(loan_amount)),
        "median_salary": float(np.median(salary)),
        "interest_rate_90th_percentile": float(np.percentile(interest, 90)),
        "salary_loan_correlation": float(np.corrcoef(salary, loan_amount)[0, 1]),
        "loan_amount_std_dev": float(np.std(loan_amount)),
        "salary_std_dev": float(np.std(salary)),
    }
    return stats


# --------------------------------------------------------------------------- #
# Cleaning: missing values and outliers
# --------------------------------------------------------------------------- #
def clean_missing(df: pd.DataFrame) -> pd.DataFrame:
    """Impute missing values per the brief.

    Salary       -> median
    CreditScore  -> mean
    InterestRate -> previous value (forward fill)
    """
    df = df.copy()
    df["Salary"] = df["Salary"].fillna(df["Salary"].median())
    df["CreditScore"] = df["CreditScore"].fillna(df["CreditScore"].mean())
    df["InterestRate"] = df["InterestRate"].ffill().bfill()
    return df


def remove_outliers(df: pd.DataFrame) -> tuple[pd.DataFrame, float]:
    """Remove loans whose amount exceeds the 99th percentile."""
    cutoff = float(np.percentile(df["LoanAmount"].to_numpy(dtype=float), 99))
    cleaned = df[df["LoanAmount"] <= cutoff].copy()
    return cleaned, cutoff


# --------------------------------------------------------------------------- #
# Finance metrics
# --------------------------------------------------------------------------- #
def add_finance_metrics(df: pd.DataFrame) -> pd.DataFrame:
    """Add per-loan finance metrics using the Loan class."""
    df = df.copy()

    dti, utilization, outstanding, exp_loss = [], [], [], []
    for row in df.itertuples(index=False):
        loan = Loan(
            loan_id=row.LoanID,
            customer_id=row.CustomerID,
            loan_amount=row.LoanAmount,
            interest_rate=row.InterestRate,
            tenure=row.Tenure,
            emi=row.EMI,
            paid_emis=row.PaidEMIs,
            default_flag=row.DefaultFlag,
            salary=row.Salary,
            credit_score=row.CreditScore,
        )
        dti.append(loan.debt_to_income())
        utilization.append(loan.loan_utilization())
        outstanding.append(loan.outstanding)
        exp_loss.append(loan.expected_loss())

    df["Outstanding"] = outstanding
    df["DebtToIncome"] = dti
    df["LoanUtilization"] = utilization
    df["ExpectedLoss"] = exp_loss
    return df


def portfolio_metrics(df: pd.DataFrame) -> dict:
    """Aggregate portfolio-level finance metrics."""
    total_loans = len(df)
    defaults = int(df["DefaultFlag"].sum())
    npa_amount = float(df.loc[df["DefaultFlag"] == 1, "Outstanding"].sum())
    total_outstanding = float(df["Outstanding"].sum())

    return {
        "total_loans": total_loans,
        "average_dti": float(df["DebtToIncome"].mean()),
        "average_loan_utilization": float(df["LoanUtilization"].mean()),
        "default_percentage": float(defaults / total_loans * 100) if total_loans else 0.0,
        "npa_percentage": float(npa_amount / total_outstanding * 100) if total_outstanding else 0.0,
        "average_emi": float(df["EMI"].mean()),
        "total_expected_loss": float(df["ExpectedLoss"].sum()),
    }


# --------------------------------------------------------------------------- #
# Risk identification
# --------------------------------------------------------------------------- #
def _normalize(series: pd.Series, invert: bool = False) -> pd.Series:
    """Min-max scale a series to [0, 1]; invert so that 'worse' -> higher."""
    lo, hi = series.min(), series.max()
    if hi == lo:
        scaled = pd.Series(0.0, index=series.index)
    else:
        scaled = (series - lo) / (hi - lo)
    return (1 - scaled) if invert else scaled


def add_risk_score(df: pd.DataFrame) -> pd.DataFrame:
    """Build a composite 0-100 risk score (higher = riskier).

    Also flags the four hard criteria from the brief:
      CreditScore < 650, Salary < 60,000, Loan > 10 Lakhs, DefaultFlag = 1.
    """
    df = df.copy()

    df["MeetsHardCriteria"] = (
        (df["CreditScore"] < CREDIT_SCORE_THRESHOLD)
        & (df["Salary"] < SALARY_THRESHOLD)
        & (df["LoanAmount"] > LOAN_THRESHOLD)
        & (df["DefaultFlag"] == 1)
    )

    # Weighted blend of risk drivers.
    score = (
        0.30 * _normalize(df["CreditScore"], invert=True)   # lower score -> riskier
        + 0.15 * _normalize(df["Salary"], invert=True)       # lower salary -> riskier
        + 0.15 * _normalize(df["LoanAmount"])                # bigger loan -> riskier
        + 0.20 * df["DefaultFlag"].astype(float)             # default -> riskier
        + 0.10 * _normalize(df["DebtToIncome"])              # higher DTI -> riskier
        + 0.10 * _normalize(df["LoanUtilization"])           # more outstanding -> riskier
    )
    df["RiskScore"] = (score * 100).round(2)
    return df


def find_high_risk_customers(df: pd.DataFrame) -> pd.DataFrame:
    """Return the Top-N riskiest customers ranked by composite risk score."""
    ranked = df.sort_values("RiskScore", ascending=False)
    return ranked.head(TOP_N_RISKY)


# --------------------------------------------------------------------------- #
# Report generation
# --------------------------------------------------------------------------- #
def generate_reports(
    portfolio: pd.DataFrame,
    high_risk: pd.DataFrame,
    stats: dict,
    metrics: dict,
    outlier_cutoff: float,
) -> None:
    """Write the Excel, CSV and JSON deliverables."""

    # 1) high_risk_customers.csv
    high_risk.to_csv(HIGH_RISK_CSV, index=False)

    # 2) risk_report.xlsx (multi-sheet)
    summary_rows = (
        [{"Metric": k, "Value": v} for k, v in stats.items()]
        + [{"Metric": k, "Value": v} for k, v in metrics.items()]
        + [{"Metric": "loan_amount_99th_percentile_cutoff", "Value": outlier_cutoff}]
    )
    summary_df = pd.DataFrame(summary_rows)

    with pd.ExcelWriter(XLSX_REPORT, engine="openpyxl") as writer:
        summary_df.to_excel(writer, sheet_name="Summary", index=False)
        portfolio.to_excel(writer, sheet_name="Portfolio", index=False)
        high_risk.to_excel(writer, sheet_name="HighRiskCustomers", index=False)

    # 3) summary.json
    summary_payload = {
        "numpy_statistics": stats,
        "finance_metrics": metrics,
        "outlier_cutoff_loan_amount_99pct": outlier_cutoff,
        "high_risk_customer_count": int(len(high_risk)),
        "high_risk_customer_ids": high_risk["CustomerID"].tolist(),
    }
    with open(SUMMARY_JSON, "w", encoding="utf-8") as fh:
        json.dump(summary_payload, fh, indent=2)


# --------------------------------------------------------------------------- #
# Orchestration
# --------------------------------------------------------------------------- #
def main() -> None:
    print("Loading source files...")
    customers, loans, credit = load_data()

    print("Merging datasets...")
    merged = (
        loans.merge(customers, on="CustomerID", how="left")
        .merge(credit, on="CustomerID", how="left")
    )

    print("Cleaning missing values...")
    merged = clean_missing(merged)

    print("Removing loan-amount outliers (>99th percentile)...")
    merged, cutoff = remove_outliers(merged)

    print("Calculating NumPy statistics...")
    stats = numpy_statistics(merged)

    print("Engineering finance metrics...")
    merged = add_finance_metrics(merged)
    metrics = portfolio_metrics(merged)

    print("Scoring and identifying high-risk customers...")
    merged = add_risk_score(merged)
    metrics["customers_meeting_all_hard_criteria"] = int(merged["MeetsHardCriteria"].sum())
    high_risk = find_high_risk_customers(merged)

    print("Generating reports...")
    generate_reports(merged, high_risk, stats, metrics, cutoff)

    print("\n=== NumPy Statistics ===")
    for k, v in stats.items():
        print(f"  {k}: {v:,.4f}")

    print("\n=== Finance Metrics ===")
    for k, v in metrics.items():
        print(f"  {k}: {v:,.4f}")

    print(f"\nHigh-risk customers found: {len(high_risk)}")
    print("\nReports written:")
    print(f"  - {XLSX_REPORT}")
    print(f"  - {HIGH_RISK_CSV}")
    print(f"  - {SUMMARY_JSON}")


if __name__ == "__main__":
    try:
        main()
    except (FileNotFoundError, ValueError) as exc:
        print(f"[ERROR] {exc}")
        raise SystemExit(1) from exc
