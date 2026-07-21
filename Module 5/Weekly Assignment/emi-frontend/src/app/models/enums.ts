// String enums mirroring the backend enum types.
export type Role = 'USER' | 'UNDERWRITER' | 'MANAGER' | 'ADMIN';
export type LoanType = 'PERSONAL' | 'HOME' | 'VEHICLE' | 'EDUCATION' | 'BUSINESS';
export type LoanStatus = 'PENDING' | 'ACTIVE' | 'CLOSED' | 'DEFAULTED';
export type PaymentMode = 'UPI' | 'CARD' | 'NETBANKING' | 'CASH';
export type PaymentStatus = 'PAID' | 'MISSED' | 'PENDING';
export type CustomerStatus = 'ACTIVE' | 'INACTIVE';

// New spec-aligned enums.
export type LoanApplicationStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';
export type LoanAccountStatus = 'APPROVED' | 'DISBURSED' | 'ACTIVE' | 'CLOSED';
export type EmiPaymentStatus = 'PENDING' | 'PAID' | 'OVERDUE';
export type PaymentType = 'CASH' | 'CARD' | 'ONLINE' | 'UPI';

// Ready-made option lists for <select> dropdowns.
export const ROLES: Role[] = ['USER', 'UNDERWRITER', 'MANAGER', 'ADMIN'];
export const LOAN_TYPES: LoanType[] = ['PERSONAL', 'HOME', 'VEHICLE', 'EDUCATION', 'BUSINESS'];
export const PAYMENT_MODES: PaymentMode[] = ['UPI', 'CARD', 'NETBANKING', 'CASH'];
export const PAYMENT_STATUSES: PaymentStatus[] = ['PAID', 'MISSED', 'PENDING'];
export const PAYMENT_TYPES: PaymentType[] = ['UPI', 'CARD', 'ONLINE', 'CASH'];
export const LOAN_APPLICATION_STATUSES: LoanApplicationStatus[] = [
  'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED',
];
export const LOAN_ACCOUNT_STATUSES: LoanAccountStatus[] = [
  'APPROVED', 'DISBURSED', 'ACTIVE', 'CLOSED',
];
