import { EmiPaymentStatus, PaymentType } from './enums';

// Mirrors EmiPaymentResponse from the backend.
export interface EmiPayment {
  paymentId: number;
  loanAccountId?: number;
  installmentNo: number;
  dueDate?: string;
  paymentDate?: string;
  emiAmount: number;
  principalPaid?: number;
  interestPaid?: number;
  penaltyPaid?: number;
  totalPaid?: number;
  paymentType?: PaymentType;
  status: EmiPaymentStatus;
}

// Mirrors EmiPaymentRequest. installmentNo is optional (backend pays next due when null).
export interface EmiPaymentRequest {
  installmentNo?: number;
  paymentType: PaymentType;
}
