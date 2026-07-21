import { LoanAccountStatus } from './enums';

// Mirrors LoanAccountResponse from the backend.
export interface LoanAccount {
  loanAccountId: number;
  loanNumber: string;
  applicationId?: number;
  customerId?: number;
  customerName?: string;
  approvedAmount: number;
  interestRate: number;
  tenureMonths: number;
  emiAmount: number;
  applicationDate?: string;
  approvalDate?: string;
  disbursementDate?: string;
  loanStartDate?: string;
  nextEmiDate?: string;
  loanCloseDate?: string;
  status: LoanAccountStatus;
}
