// Aggregated analytics returned by GET /dashboard.
export interface DashboardDto {
  totalCustomers: number;
  activeLoans: number;
  closedLoans: number;
  totalEMICollected: number;
  totalPenaltyCollected: number;
  topBranch: string;
  highestPayingCustomer: string;
  highestLoanAmount: number;
  defaultedLoans: number;
}
