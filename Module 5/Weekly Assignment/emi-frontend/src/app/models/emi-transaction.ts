import { PaymentMode, PaymentStatus } from './enums';

export interface EmiTransaction {
  transactionId?: number;
  installmentNumber: number;
  amountPaid: number;
  paymentDate: string; // ISO date string (yyyy-MM-dd)
  paymentMode: PaymentMode;
  paymentStatus: PaymentStatus;
}
