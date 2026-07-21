// Projection DTOs returned by the /loans analytics endpoints.
export interface BranchCollection {
  branchName: string;
  totalCollected: number;
}

export interface CustomerSummary {
  customerName: string;
  branchName: string;
  numberOfLoans: number;
  totalEMIPaid: number;
  totalPenaltyPaid: number;
}

export interface MonthlyCollection {
  year: number;
  month: number;
  totalCollected: number;
}
