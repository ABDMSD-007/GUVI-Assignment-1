import { Customer } from './customer';
import { LoanStatus, LoanType } from './enums';

export interface Loan {
  loanId: number;
  loanType: LoanType;
  principalAmount: number;
  interestRate: number;
  tenureMonths: number;
  emiAmount: number;
  loanStatus: LoanStatus;
  active: boolean;
  customer?: Customer;
}
