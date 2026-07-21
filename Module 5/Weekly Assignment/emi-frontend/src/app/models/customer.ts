import { Role } from './enums';

export interface Customer {
  customerId?: number;
  customerName: string;
  email: string;
  password?: string;
  mobileNumber: string;
  branchName: string;
  creditScore?: number;
  role?: Role;
}
