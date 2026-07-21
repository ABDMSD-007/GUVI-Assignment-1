import { LoanApplicationStatus } from './enums';

// Mirrors LoanApplicationResponse from the backend.
export interface LoanApplication {
  applicationId: number;
  customerId?: number;
  customerName?: string;
  loanCode: string;
  loanName?: string;
  requestedAmount: number;
  tenureMonths: number;
  applicationDate?: string;
  status: LoanApplicationStatus;
  remarks?: string;
  loanAccountId?: number;
}

// Mirrors LoanApplicationRequest (customer applies).
export interface LoanApplicationRequest {
  loanCode: string;
  requestedAmount: number;
  tenureMonths: number;
}
