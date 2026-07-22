"""
Mutual Fund Portfolio Performance & Risk Analysis
=================================================
An automated analytics dashboard for an Asset Management Company (AMC).

Dataset schema
--------------
investors.csv    : InvestorID, InvestorName, Age, City, AnnualIncome, RiskProfile
funds.csv        : FundID, FundName, Category, FundManager, ExpenseRatio, Benchmark
transactions.csv : TransactionID, InvestorID, FundID, TransactionDate,
                   TransactionType, Units, NAV, Amount
nav_history.csv  : FundID, Date, NAV

Pipeline
--------
1.  Read all CSV files (robust exception handling)
2.  Coerce types + clean/impute missing values
3.  Remove duplicate transactions
4.  Remove outliers (investment > 99th pct, NAV change > 3 sigma)
5.  NumPy statistics
6.  Merge investors + transactions + funds + nav_history
7.  Rank funds and identify high-value investors
8.  Compute portfolio / finance metrics (OOP: FundPortfolio)
9.  Generate charts
10. Export Excel / CSV / JSON reports and log execution status

All deliverables are written to the ``output`` folder next to this script.
"""

from __future__ import annotations

import json
import logging
import os
import time
from dataclasses import dataclass

import matplotlib

matplotlib.use("Agg")  # headless backend (no display required)
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

# --------------------------------------------------------------------------- #
# Configuration
# --------------------------------------------------------------------------- #
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
OUTPUT_DIR = os.path.join(BASE_DIR, "output")
CHART_DIR = os.path.join(OUTPUT_DIR, "charts")

FILES = {
    "investors": os.path.join(BASE_DIR, "investors.csv"),
    "funds": os.path.join(BASE_DIR, "funds.csv"),
    "transactions": os.path.join(BASE_DIR, "transactions.csv"),
    "nav_history": os.path.join(BASE_DIR, "nav_history.csv"),
}

REQUIRED_COLS = {
    "investors": ["InvestorID", "InvestorName", "Age", "City", "AnnualIncome", "RiskProfile"],
    "funds": ["FundID", "FundName", "Category", "FundManager", "ExpenseRatio", "Benchmark"],
    "transactions": ["TransactionID", "InvestorID", "FundID", "TransactionDate",
                     "TransactionType", "Units", "NAV", "Amount"],
    "nav_history": ["FundID", "Date", "NAV"],
}

# High-value investor thresholds (from the brief)
INVESTMENT_THRESHOLD = 1_000_000     # > 10 Lakhs invested
INCOME_THRESHOLD = 1_500_000         # > 15 Lakhs annual income
MIN_TRANSACTIONS = 10                # more than 10 transactions
RISK_HIGH = "High"
TOP_N_INVESTORS = 20

# Finance assumptions
RISK_FREE_RATE = 0.06                # 6% annual, for the Sharpe ratio

os.makedirs(CHART_DIR, exist_ok=True)

# --------------------------------------------------------------------------- #
# Logging
# --------------------------------------------------------------------------- #
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)-7s | %(message)s",
    handlers=[
        logging.FileHandler(os.path.join(OUTPUT_DIR, "execution.log"), mode="w"),
        logging.StreamHandler(),
    ],
)
logger = logging.getLogger("mf_dashboard")
logging.getLogger("matplotlib").setLevel(logging.WARNING)


# --------------------------------------------------------------------------- #
# OOP: FundPortfolio
# --------------------------------------------------------------------------- #
@dataclass
class FundPortfolio:
    """Encapsulates a merged, cleaned mutual-fund portfolio and its analytics."""

    holdings: pd.DataFrame          # one row per investor-fund holding
    funds: pd.DataFrame             # fund master + derived performance
    nav: pd.DataFrame               # cleaned NAV history
    valuation_date: pd.Timestamp    # date at which holdings are valued

    # ---- headline figures -------------------------------------------- #
    def total_portfolio_value(self) -> float:
        return float(self.holdings["CurrentValue"].sum())

    def total_invested(self) -> float:
        return float(self.holdings["Invested"].sum())

    def absolute_return(self) -> float:
        return self.total_portfolio_value() - self.total_invested()

    def portfolio_return_pct(self) -> float:
        invested = self.total_invested()
        return (self.absolute_return() / invested * 100) if invested else 0.0

    # ---- time-weighted returns --------------------------------------- #
    def average_holding_period_days(self) -> float:
        return float(self.holdings["HoldingDays"].mean())

    def portfolio_span_days(self) -> int:
        span = (self.valuation_date - self.holdings["FirstDate"].min()).days
        return int(max(span, 1))

    def cagr_pct(self) -> float:
        """Compound annual growth rate over the full portfolio span."""
        years = max(self.portfolio_span_days() / 365.0, 1e-9)
        growth = self.total_portfolio_value() / max(self.total_invested(), 1e-9)
        return (growth ** (1 / years) - 1) * 100

    def annualized_return_pct(self) -> float:
        """Return annualised over the average holding period."""
        years = max(self.average_holding_period_days() / 365.0, 1e-9)
        growth = self.total_portfolio_value() / max(self.total_invested(), 1e-9)
        return (growth ** (1 / years) - 1) * 100

    # ---- risk / diversification -------------------------------------- #
    def diversification_score(self) -> float:
        """1 - Herfindahl index over fund allocation (0=concentrated, 1=diversified)."""
        alloc = self.holdings.groupby("FundID")["CurrentValue"].sum()
        total = alloc.sum()
        if total <= 0:
            return 0.0
        weights = alloc / total
        return 1 - float((weights ** 2).sum())

    def sharpe_ratio(self) -> float:
        """Simplified Sharpe: (mean holding return - risk-free) / std of holding returns."""
        r = self.holdings["ReturnPct"].dropna() / 100.0
        if r.empty or r.std(ddof=0) == 0:
            return 0.0
        return float((r.mean() - RISK_FREE_RATE) / r.std(ddof=0))

    def expense_ratio_impact(self) -> float:
        """Annual cost drag = sum(current value * expense ratio %)."""
        merged = self.holdings.merge(
            self.funds[["FundID", "ExpenseRatio"]], on="FundID", how="left"
        )
        return float((merged["CurrentValue"] * merged["ExpenseRatio"] / 100).sum())

    # ---- allocation breakdowns --------------------------------------- #
    def category_wise_investment_pct(self) -> pd.Series:
        merged = self.holdings.merge(self.funds[["FundID", "Category"]], on="FundID", how="left")
        by_cat = merged.groupby("Category")["Invested"].sum()
        return (by_cat / by_cat.sum() * 100).round(2).sort_values(ascending=False)

    def fund_allocation_pct(self) -> pd.Series:
        alloc = self.holdings.groupby("FundID")["CurrentValue"].sum()
        return (alloc / alloc.sum() * 100).round(2).sort_values(ascending=False)

    def investor_profit_loss(self) -> pd.DataFrame:
        pnl = (
            self.holdings.groupby("InvestorID")
            .agg(Invested=("Invested", "sum"), CurrentValue=("CurrentValue", "sum"))
            .reset_index()
        )
        pnl["ProfitLoss"] = pnl["CurrentValue"] - pnl["Invested"]
        pnl["ReturnPct"] = np.where(
            pnl["Invested"] > 0, pnl["ProfitLoss"] / pnl["Invested"] * 100, 0.0
        ).round(2)
        return pnl.sort_values("ProfitLoss", ascending=False)


# --------------------------------------------------------------------------- #
# 1. Data loading (with exception handling)
# --------------------------------------------------------------------------- #
def read_csv_safe(name: str, path: str, required: list[str]) -> pd.DataFrame:
    """Read a CSV robustly, tolerating corrupted rows, stray whitespace and encodings."""
    if not os.path.exists(path):
        raise FileNotFoundError(f"Input file not found: {path}")
    try:
        df = pd.read_csv(path, on_bad_lines="skip", skip_blank_lines=True,
                         skipinitialspace=True)
    except pd.errors.EmptyDataError as exc:
        raise ValueError(f"File is empty: {path}") from exc
    except pd.errors.ParserError as exc:
        raise ValueError(f"File is corrupted and cannot be parsed: {path}") from exc
    except UnicodeDecodeError:
        logger.warning("%s: falling back to latin-1 encoding", name)
        df = pd.read_csv(path, on_bad_lines="skip", encoding="latin-1")

    # Normalise headers and trim stray whitespace/tabs from string cells.
    df.columns = [c.strip() for c in df.columns]
    for col in df.columns:
        if df[col].dtype == object or pd.api.types.is_string_dtype(df[col]):
            df[col] = df[col].astype(str).str.strip()

    missing = [c for c in required if c not in df.columns]
    if missing:
        raise ValueError(f"{name}.csv missing required columns: {missing}")
    logger.info("Loaded %-13s %4d rows", name, len(df))
    return df


def load_all() -> dict[str, pd.DataFrame]:
    return {name: read_csv_safe(name, path, REQUIRED_COLS[name])
            for name, path in FILES.items()}


# --------------------------------------------------------------------------- #
# 2. Type coercion & missing-value imputation
# --------------------------------------------------------------------------- #
def coerce_types(data: dict[str, pd.DataFrame]) -> dict[str, pd.DataFrame]:
    inv, fun, txn, nav = (data["investors"], data["funds"],
                          data["transactions"], data["nav_history"])

    inv["Age"] = pd.to_numeric(inv["Age"], errors="coerce")
    inv["AnnualIncome"] = pd.to_numeric(inv["AnnualIncome"], errors="coerce")

    fun["ExpenseRatio"] = pd.to_numeric(fun["ExpenseRatio"], errors="coerce")

    for col in ("Units", "NAV", "Amount"):
        txn[col] = pd.to_numeric(txn[col], errors="coerce")
    txn["TransactionDate"] = pd.to_datetime(txn["TransactionDate"], errors="coerce")

    nav["NAV"] = pd.to_numeric(nav["NAV"], errors="coerce")
    nav["Date"] = pd.to_datetime(nav["Date"], errors="coerce")

    data["investors"], data["funds"], data["transactions"], data["nav_history"] = inv, fun, txn, nav
    return data


def clean_missing(data: dict[str, pd.DataFrame]) -> dict[str, pd.DataFrame]:
    """Impute missing values exactly as the brief specifies."""
    inv, fun, txn, nav = (data["investors"], data["funds"],
                          data["transactions"], data["nav_history"])

    # Annual Income -> median ; Risk Profile -> "Moderate"
    inv["AnnualIncome"] = inv["AnnualIncome"].fillna(inv["AnnualIncome"].median())
    inv["RiskProfile"] = inv["RiskProfile"].fillna("Moderate")

    # Expense Ratio -> mean
    fun["ExpenseRatio"] = fun["ExpenseRatio"].fillna(fun["ExpenseRatio"].mean())

    # NAV -> previous day's NAV (forward fill per fund)
    nav = nav.sort_values(["FundID", "Date"])
    nav["NAV"] = nav.groupby("FundID")["NAV"].ffill().bfill()

    # Defensive imputation for transaction numerics; recompute Amount if missing.
    txn["Units"] = txn["Units"].fillna(txn["Units"].median())
    txn["NAV"] = txn["NAV"].fillna(txn["NAV"].median())
    txn["Amount"] = txn["Amount"].fillna(txn["Units"] * txn["NAV"])

    data["investors"], data["funds"], data["transactions"], data["nav_history"] = inv, fun, txn, nav
    logger.info("Imputed missing values (income->median, expense->mean, "
                "NAV->previous day, risk->Moderate)")
    return data


def remove_duplicate_transactions(txn: pd.DataFrame) -> pd.DataFrame:
    before = len(txn)
    txn = txn.drop_duplicates(subset=["TransactionID"])
    txn = txn.drop_duplicates(
        subset=["InvestorID", "FundID", "TransactionDate", "TransactionType", "Units", "Amount"]
    )
    logger.info("Removed %d duplicate transactions", before - len(txn))
    return txn


def remove_outliers(txn: pd.DataFrame, nav: pd.DataFrame) -> tuple[pd.DataFrame, pd.DataFrame, dict]:
    """Drop investment amounts above the 99th percentile and NAV moves beyond 3 sigma."""
    info: dict = {}

    cutoff = float(np.percentile(txn["Amount"].to_numpy(dtype=float), 99))
    before = len(txn)
    txn = txn[txn["Amount"] <= cutoff].copy()
    info["amount_99pct_cutoff"] = round(cutoff, 2)
    info["amount_outliers_removed"] = before - len(txn)

    nav = nav.sort_values(["FundID", "Date"]).copy()
    nav["NAVChange"] = nav.groupby("FundID")["NAV"].pct_change()
    std = nav["NAVChange"].std(ddof=0)
    mean = nav["NAVChange"].mean()
    if std and not np.isnan(std):
        keep = (nav["NAVChange"].abs() <= (abs(mean) + 3 * std)) | nav["NAVChange"].isna()
    else:
        keep = pd.Series(True, index=nav.index)
    nav_before = len(nav)
    nav = nav[keep].drop(columns=["NAVChange"]).copy()
    info["nav_outliers_removed"] = nav_before - len(nav)

    logger.info("Removed %d amount outliers (>99pct=%.2f) and %d NAV-change outliers (>3 sigma)",
                info["amount_outliers_removed"], cutoff, info["nav_outliers_removed"])
    return txn, nav, info


# --------------------------------------------------------------------------- #
# 3. NumPy statistics
# --------------------------------------------------------------------------- #
def numpy_statistics(inv: pd.DataFrame, txn: pd.DataFrame, nav: pd.DataFrame,
                     funds_perf: pd.DataFrame) -> dict:
    amount = txn["Amount"].to_numpy(dtype=float)
    income = inv["AnnualIncome"].to_numpy(dtype=float)
    nav_vals = nav["NAV"].to_numpy(dtype=float)
    fund_returns = funds_perf["NAVReturnPct"].dropna().to_numpy(dtype=float)

    # Correlation between annual income and total invested per investor.
    per_investor = txn.groupby("InvestorID")["Amount"].sum().rename("Invested").reset_index()
    per_investor = per_investor.merge(inv[["InvestorID", "AnnualIncome"]],
                                      on="InvestorID", how="left") \
        .dropna(subset=["AnnualIncome", "Invested"])
    income_corr = (
        float(np.corrcoef(per_investor["AnnualIncome"], per_investor["Invested"])[0, 1])
        if len(per_investor) > 1 else float("nan")
    )

    stats = {
        "mean_investment_amount": round(float(np.mean(amount)), 2),
        "median_investor_income": round(float(np.median(income)), 2),
        "std_dev_nav": round(float(np.std(nav_vals)), 4),
        "fund_returns_90th_percentile": round(float(np.percentile(fund_returns, 90)), 4) if fund_returns.size else None,
        "fund_returns_95th_percentile": round(float(np.percentile(fund_returns, 95)), 4) if fund_returns.size else None,
        "income_investment_correlation": round(income_corr, 4),
        "average_daily_nav": round(float(np.mean(nav_vals)), 4),
    }
    logger.info("Computed NumPy statistics")
    return stats


# --------------------------------------------------------------------------- #
# 4. Fund performance & ranking
# --------------------------------------------------------------------------- #
def build_fund_performance(funds: pd.DataFrame, nav: pd.DataFrame,
                           holdings: pd.DataFrame, txn: pd.DataFrame) -> pd.DataFrame:
    """Attach NAV-based returns, AUM (current market value) and popularity to funds."""
    nav = nav.sort_values(["FundID", "Date"])
    first = nav.groupby("FundID")["NAV"].first()
    last = nav.groupby("FundID")["NAV"].last()
    nav_return = ((last - first) / first * 100).rename("NAVReturnPct")

    aum = holdings.groupby("FundID")["CurrentValue"].sum().rename("AUM")
    popularity = txn.groupby("FundID")["TransactionID"].count().rename("TransactionCount")

    perf = (
        funds.merge(nav_return, on="FundID", how="left")
        .merge(aum, on="FundID", how="left")
        .merge(popularity, on="FundID", how="left")
    )
    perf["AUM"] = perf["AUM"].fillna(0.0)
    perf["TransactionCount"] = perf["TransactionCount"].fillna(0).astype(int)
    return perf


def rank_funds(perf: pd.DataFrame) -> dict:
    with_returns = perf.dropna(subset=["NAVReturnPct"])
    best = with_returns.loc[with_returns["NAVReturnPct"].idxmax()] if not with_returns.empty else None
    worst = with_returns.loc[with_returns["NAVReturnPct"].idxmin()] if not with_returns.empty else None
    highest_expense = perf.loc[perf["ExpenseRatio"].idxmax()]
    highest_aum = perf.loc[perf["AUM"].idxmax()]
    most_popular = perf.loc[perf["TransactionCount"].idxmax()]

    def _fmt(row, col, label):
        if row is None:
            return None
        return {"FundID": row["FundID"], "FundName": row["FundName"],
                "Category": row["Category"], label: round(float(row[col]), 2)}

    ranking = {
        "best_performing_fund": _fmt(best, "NAVReturnPct", "ReturnPct"),
        "worst_performing_fund": _fmt(worst, "NAVReturnPct", "ReturnPct"),
        "highest_expense_ratio_fund": _fmt(highest_expense, "ExpenseRatio", "ExpenseRatio"),
        "highest_aum_fund": _fmt(highest_aum, "AUM", "AUM"),
        "most_popular_fund": _fmt(most_popular, "TransactionCount", "TransactionCount"),
    }
    logger.info("Ranked funds (best / worst / expense / AUM / popular)")
    return ranking


# --------------------------------------------------------------------------- #
# 5. Holdings & high-value investors
# --------------------------------------------------------------------------- #
def build_holdings(txn: pd.DataFrame, nav: pd.DataFrame,
                   valuation_date: pd.Timestamp) -> pd.DataFrame:
    """Aggregate Buy/Sell transactions into net investor-fund holdings, valued at latest NAV."""
    txn = txn.copy()
    sign = np.where(txn["TransactionType"].str.lower().eq("sell"), -1, 1)
    txn["SignedUnits"] = txn["Units"] * sign
    txn["SignedAmount"] = txn["Amount"] * sign

    # Current NAV per fund: latest market NAV, else last transaction NAV.
    nav_latest = nav.sort_values("Date").groupby("FundID")["NAV"].last()
    txn_latest = txn.sort_values("TransactionDate").groupby("FundID")["NAV"].last()
    current_nav = txn_latest.copy()
    current_nav.update(nav_latest)

    grp = txn.groupby(["InvestorID", "FundID"]).agg(
        Units=("SignedUnits", "sum"),
        Invested=("SignedAmount", "sum"),
        FirstDate=("TransactionDate", "min"),
        LastDate=("TransactionDate", "max"),
        TxnCount=("TransactionID", "count"),
    ).reset_index()

    grp["CurrentNAV"] = grp["FundID"].map(current_nav)
    grp["CurrentValue"] = grp["Units"] * grp["CurrentNAV"]
    grp["ProfitLoss"] = grp["CurrentValue"] - grp["Invested"]
    grp["ReturnPct"] = np.where(
        grp["Invested"] > 0, grp["ProfitLoss"] / grp["Invested"] * 100, 0.0
    )
    grp["HoldingDays"] = (valuation_date - grp["FirstDate"]).dt.days.clip(lower=1)
    return grp


def aggregate_investors(holdings: pd.DataFrame, inv: pd.DataFrame) -> pd.DataFrame:
    """Portfolio value / P&L per investor, ranked, with the high-value flag from the brief."""
    agg = holdings.groupby("InvestorID").agg(
        PortfolioValue=("CurrentValue", "sum"),
        TotalInvested=("Invested", "sum"),
        ProfitLoss=("ProfitLoss", "sum"),
        TransactionCount=("TxnCount", "sum"),
    ).reset_index()

    agg["ReturnPct"] = np.where(
        agg["TotalInvested"] > 0, agg["ProfitLoss"] / agg["TotalInvested"] * 100, 0.0
    ).round(2)
    agg = agg.merge(inv, on="InvestorID", how="left")

    # High-value: Investment > 10L AND High risk AND > 10 txns AND Income > 15L
    agg["HighValue"] = (
        (agg["TotalInvested"] > INVESTMENT_THRESHOLD)
        & (agg["RiskProfile"] == RISK_HIGH)
        & (agg["TransactionCount"] > MIN_TRANSACTIONS)
        & (agg["AnnualIncome"] > INCOME_THRESHOLD)
    )
    return agg.sort_values("PortfolioValue", ascending=False)


# --------------------------------------------------------------------------- #
# 6. Charts
# --------------------------------------------------------------------------- #
def _save(fig, filename: str, paths: list[str]) -> None:
    p = os.path.join(CHART_DIR, filename)
    fig.savefig(p, bbox_inches="tight")
    plt.close(fig)
    paths.append(p)


def generate_charts(portfolio: FundPortfolio, holdings: pd.DataFrame,
                    txn: pd.DataFrame, perf: pd.DataFrame,
                    investor_agg: pd.DataFrame) -> list[str]:
    paths: list[str] = []

    # 1) Portfolio allocation by category (pie)
    cat_pct = portfolio.category_wise_investment_pct()
    fig, ax = plt.subplots(figsize=(7, 7))
    ax.pie(cat_pct.values, labels=cat_pct.index, autopct="%1.1f%%", startangle=90)
    ax.set_title("Portfolio Allocation by Category")
    _save(fig, "portfolio_allocation_pie.png", paths)

    # 2) Fund-wise investment (bar)
    fund_inv = holdings.merge(perf[["FundID", "FundName"]], on="FundID", how="left") \
        .groupby("FundName")["Invested"].sum().sort_values(ascending=False)
    fig, ax = plt.subplots(figsize=(10, 5))
    ax.bar(fund_inv.index, fund_inv.values, color="steelblue")
    ax.set_title("Fund-wise Investment"); ax.set_ylabel("Invested (INR)")
    plt.xticks(rotation=40, ha="right")
    _save(fig, "fundwise_investment_bar.png", paths)

    # 3) Investment trend (line) - adapts to the data span so it never collapses
    #    to a single point. Uses monthly buckets when the data spans multiple
    #    months, otherwise daily buckets.
    dates = txn["TransactionDate"]
    if dates.dt.to_period("M").nunique() > 1:
        trend = txn.groupby(dates.dt.to_period("M"))["Amount"].sum()
        trend.index = trend.index.astype(str)
        title = "Monthly Investment Trend"
    else:
        trend = txn.groupby(dates.dt.date)["Amount"].sum()
        title = "Daily Investment Trend"
    fig, ax = plt.subplots(figsize=(10, 5))
    ax.plot(trend.index.astype(str), trend.values, marker="o", color="darkgreen")
    ax.set_title(title); ax.set_ylabel("Amount (INR)")
    plt.xticks(rotation=40, ha="right")
    _save(fig, "monthly_investment_trend_line.png", paths)

    # 4) Category-wise average returns (bar)
    cat_ret = perf.dropna(subset=["NAVReturnPct"]).groupby("Category")["NAVReturnPct"].mean() \
        .sort_values(ascending=False)
    fig, ax = plt.subplots(figsize=(9, 5))
    ax.bar(cat_ret.index, cat_ret.values, color="indianred")
    ax.set_title("Category-wise Average NAV Returns"); ax.set_ylabel("Return %")
    plt.xticks(rotation=30, ha="right")
    _save(fig, "categorywise_returns_bar.png", paths)

    # 5) NAV movement (line)
    fig, ax = plt.subplots(figsize=(10, 5))
    for fid, g in portfolio.nav.groupby("FundID"):
        ax.plot(g["Date"], g["NAV"], marker=".", label=fid)
    ax.set_title("NAV Movement"); ax.set_ylabel("NAV"); ax.legend(title="Fund")
    plt.xticks(rotation=30, ha="right")
    _save(fig, "nav_movement_line.png", paths)

    # 6) Top 10 investors by portfolio value (horizontal bar)
    top10 = investor_agg.nlargest(10, "PortfolioValue")
    fig, ax = plt.subplots(figsize=(9, 6))
    ax.barh(top10["InvestorName"][::-1], top10["PortfolioValue"][::-1], color="teal")
    ax.set_title("Top 10 Investors by Portfolio Value"); ax.set_xlabel("Portfolio Value (INR)")
    _save(fig, "top10_investors_hbar.png", paths)

    # 7) Portfolio allocation by fund (pie)
    fund_value = holdings.merge(perf[["FundID", "FundName"]], on="FundID", how="left") \
        .groupby("FundName")["CurrentValue"].sum().sort_values(ascending=False)
    fig, ax = plt.subplots(figsize=(8, 8))
    ax.pie(fund_value.values, labels=fund_value.index, autopct="%1.1f%%",
           startangle=90, pctdistance=0.82)
    ax.set_title("Portfolio Allocation by Fund")
    _save(fig, "portfolio_allocation_by_fund_pie.png", paths)

    logger.info("Generated %d charts in %s", len(paths), CHART_DIR)
    return paths


# --------------------------------------------------------------------------- #
# 7. Reports
# --------------------------------------------------------------------------- #
def _fallback_path(path: str) -> str:
    """Return a timestamped variant of *path* (used when the target is locked)."""
    root, ext = os.path.splitext(path)
    return f"{root}_{time.strftime('%Y%m%d_%H%M%S')}{ext}"


def _write_resilient(path: str, writer_fn) -> str:
    """Call ``writer_fn(target)``; if the file is locked, retry on a fallback path."""
    try:
        writer_fn(path)
        return path
    except PermissionError:
        alt = _fallback_path(path)
        writer_fn(alt)
        logger.warning("%s is open/locked elsewhere -> wrote %s instead",
                       os.path.basename(path), os.path.basename(alt))
        return alt


def export_reports(portfolio: FundPortfolio, stats: dict, ranking: dict,
                   metrics: dict, investor_agg: pd.DataFrame, perf: pd.DataFrame,
                   outlier_info: dict) -> None:
    top20 = investor_agg.head(TOP_N_INVESTORS)
    _write_resilient(
        os.path.join(OUTPUT_DIR, "top20_investors.csv"),
        lambda p: top20.to_csv(p, index=False),
    )
    high_value = investor_agg[investor_agg["HighValue"]]
    _write_resilient(
        os.path.join(OUTPUT_DIR, "high_value_investors.csv"),
        lambda p: high_value.to_csv(p, index=False),
    )

    summary_rows = (
        [{"Section": "NumPy", "Metric": k, "Value": v} for k, v in stats.items()]
        + [{"Section": "Finance", "Metric": k, "Value": v} for k, v in metrics.items()]
    )

    def _write_xlsx(p: str) -> None:
        with pd.ExcelWriter(p, engine="openpyxl") as writer:
            pd.DataFrame(summary_rows).to_excel(writer, sheet_name="Summary", index=False)
            perf.to_excel(writer, sheet_name="FundPerformance", index=False)
            portfolio.holdings.to_excel(writer, sheet_name="Holdings", index=False)
            investor_agg.to_excel(writer, sheet_name="Investors", index=False)
            portfolio.investor_profit_loss().to_excel(writer, sheet_name="InvestorPnL", index=False)
            portfolio.category_wise_investment_pct().rename("Investment%").to_frame() \
                .to_excel(writer, sheet_name="CategoryMix")
            portfolio.fund_allocation_pct().rename("Allocation%").to_frame() \
                .to_excel(writer, sheet_name="FundAllocation")

    _write_resilient(os.path.join(OUTPUT_DIR, "mutual_fund_report.xlsx"), _write_xlsx)

    payload = {
        "numpy_statistics": stats,
        "fund_ranking": ranking,
        "finance_metrics": metrics,
        "outlier_info": outlier_info,
        "category_wise_investment_pct": portfolio.category_wise_investment_pct().to_dict(),
        "fund_allocation_pct": portfolio.fund_allocation_pct().to_dict(),
        "high_value_investor_count": int(investor_agg["HighValue"].sum()),
        "top20_investor_ids": top20["InvestorID"].tolist(),
    }

    def _write_json(p: str) -> None:
        with open(p, "w", encoding="utf-8") as fh:
            json.dump(payload, fh, indent=2, default=str)

    _write_resilient(os.path.join(OUTPUT_DIR, "summary.json"), _write_json)

    logger.info("Exported Excel, CSV and JSON reports to %s", OUTPUT_DIR)


# --------------------------------------------------------------------------- #
# Orchestration
# --------------------------------------------------------------------------- #
def main() -> None:
    logger.info("=== Mutual Fund Dashboard: START ===")

    data = load_all()
    data = coerce_types(data)
    data = clean_missing(data)

    txn = remove_duplicate_transactions(data["transactions"])
    txn, nav, outlier_info = remove_outliers(txn, data["nav_history"])

    inv, funds = data["investors"], data["funds"]
    valuation_date = nav["Date"].max()
    if pd.isna(valuation_date):
        valuation_date = txn["TransactionDate"].max()

    holdings = build_holdings(txn, nav, valuation_date)
    perf = build_fund_performance(funds, nav, holdings, txn)
    stats = numpy_statistics(inv, txn, nav, perf)
    ranking = rank_funds(perf)
    investor_agg = aggregate_investors(holdings, inv)

    portfolio = FundPortfolio(holdings=holdings, funds=perf, nav=nav,
                              valuation_date=valuation_date)

    metrics = {
        "total_portfolio_value": round(portfolio.total_portfolio_value(), 2),
        "total_invested": round(portfolio.total_invested(), 2),
        "absolute_return": round(portfolio.absolute_return(), 2),
        "portfolio_return_pct": round(portfolio.portfolio_return_pct(), 2),
        "cagr_pct": round(portfolio.cagr_pct(), 2),
        "annualized_return_pct": round(portfolio.annualized_return_pct(), 2),
        "diversification_score": round(portfolio.diversification_score(), 4),
        "average_holding_period_days": round(portfolio.average_holding_period_days(), 1),
        "expense_ratio_impact": round(portfolio.expense_ratio_impact(), 2),
        "sharpe_ratio": round(portfolio.sharpe_ratio(), 4),
    }

    charts = generate_charts(portfolio, holdings, txn, perf, investor_agg)
    export_reports(portfolio, stats, ranking, metrics, investor_agg, perf, outlier_info)

    # Console summary
    logger.info("---- NumPy Statistics ----")
    for k, v in stats.items():
        logger.info("  %-32s %s", k, v)
    logger.info("---- Fund Ranking ----")
    for k, v in ranking.items():
        logger.info("  %-28s %s", k, v)
    logger.info("---- Finance Metrics ----")
    for k, v in metrics.items():
        logger.info("  %-32s %s", k, v)
    logger.info("High-value investors: %d | Charts: %d | Reports: %s",
                int(investor_agg["HighValue"].sum()), len(charts), OUTPUT_DIR)
    logger.info("=== Mutual Fund Dashboard: DONE ===")


if __name__ == "__main__":
    try:
        main()
    except (FileNotFoundError, ValueError) as exc:
        logger.error("Pipeline failed: %s", exc)
        raise SystemExit(1) from exc
    except Exception as exc:  # noqa: BLE001 - top-level safety net with full logging
        logger.exception("Unexpected error: %s", exc)
        raise SystemExit(1) from exc
