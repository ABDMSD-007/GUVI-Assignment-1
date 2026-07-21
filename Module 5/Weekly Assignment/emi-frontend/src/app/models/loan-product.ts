import { LoanType } from './enums';

// Mirrors LoanProductResponse from the backend.
export interface LoanProduct {
  loanCode: string;
  loanName: string;
  loanType: LoanType;
  minimumAmount: number;
  maximumAmount: number;
  interestRate: number;
  minimumTenure: number;
  maximumTenure: number;
  processingFee: number;
  dailyPenaltyRate: number;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

// Mirrors LoanProductRequest (create / update).
export interface LoanProductRequest {
  loanCode: string;
  loanName: string;
  loanType: LoanType;
  minimumAmount: number;
  maximumAmount: number;
  interestRate: number;
  minimumTenure: number;
  maximumTenure: number;
  processingFee: number;
  dailyPenaltyRate: number;
  active: boolean;
}
