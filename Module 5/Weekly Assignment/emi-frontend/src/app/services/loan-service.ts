import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api';
import { Loan } from '../models/loan';
import { Page } from '../models/page';
import { Customer } from '../models/customer';
import { EmiTransaction } from '../models/emi-transaction';
import { LoanType } from '../models/enums';
import {
  BranchCollection,
  CustomerSummary,
  MonthlyCollection,
} from '../models/reports';

@Injectable({
  providedIn: 'root',
})
export class LoanService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/loans`;

  // --- Core loan lifecycle -------------------------------------------------

  getLoans(page = 0, size = 10): Observable<Page<Loan>> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);
    return this.http.get<Page<Loan>>(this.baseUrl, { params });
  }

  getLoan(id: number): Observable<Loan> {
    return this.http.get<Loan>(`${this.baseUrl}/${id}`);
  }

  getSchedule(id: number): Observable<EmiTransaction[]> {
    return this.http.get<EmiTransaction[]>(`${this.baseUrl}/${id}/schedule`);
  }

  payEmi(id: number, emi: EmiTransaction): Observable<EmiTransaction> {
    return this.http.post<EmiTransaction>(`${this.baseUrl}/${id}/pay`, emi);
  }

  approve(id: number): Observable<Loan> {
    return this.http.put<Loan>(`${this.baseUrl}/${id}/approve`, {});
  }

  foreclose(id: number): Observable<Loan> {
    return this.http.put<Loan>(`${this.baseUrl}/${id}/foreclose`, {});
  }

  increaseInterest(): Observable<string> {
    return this.http.put(`${this.baseUrl}/increase-interest`, {}, {
      responseType: 'text',
    });
  }

  delete(id: number): Observable<string> {
    return this.http.delete(`${this.baseUrl}/${id}`, { responseType: 'text' });
  }

  // --- Query / analytics endpoints ----------------------------------------

  getByType(type: LoanType): Observable<Loan[]> {
    return this.http.get<Loan[]>(`${this.baseUrl}/type/${type}`);
  }

  getCustomersByBranch(branch: string): Observable<Customer[]> {
    return this.http.get<Customer[]>(`${this.baseUrl}/customers/branch/${branch}`);
  }

  getCustomersWithMinLoans(n: number): Observable<Customer[]> {
    return this.http.get<Customer[]>(`${this.baseUrl}/customers/min-loans/${n}`);
  }

  getCustomersWithMultipleLoanTypes(): Observable<Customer[]> {
    return this.http.get<Customer[]>(`${this.baseUrl}/customers/multiple-loan-types`);
  }

  getBranchCollection(): Observable<BranchCollection[]> {
    return this.http.get<BranchCollection[]>(`${this.baseUrl}/branch-collection`);
  }

  getTop10Branches(): Observable<BranchCollection[]> {
    return this.http.get<BranchCollection[]>(`${this.baseUrl}/top10-branches`);
  }

  getLatestPayment(): Observable<EmiTransaction> {
    return this.http.get<EmiTransaction>(`${this.baseUrl}/latest-payment`);
  }

  getLoansWithoutPenalty(): Observable<Loan[]> {
    return this.http.get<Loan[]>(`${this.baseUrl}/no-penalty`);
  }

  getOverdueLoans(): Observable<Loan[]> {
    return this.http.get<Loan[]>(`${this.baseUrl}/overdue`);
  }

  getEligibleCustomers(): Observable<Customer[]> {
    return this.http.get<Customer[]>(`${this.baseUrl}/eligible-customers`);
  }

  getCustomerSummaries(): Observable<CustomerSummary[]> {
    return this.http.get<CustomerSummary[]>(`${this.baseUrl}/customer-summaries`);
  }

  getMonthlyReport(): Observable<MonthlyCollection[]> {
    return this.http.get<MonthlyCollection[]>(`${this.baseUrl}/monthly-report`);
  }

  getTop5CustomersByEmi(): Observable<unknown[]> {
    return this.http.get<unknown[]>(`${this.baseUrl}/top5-emi`);
  }
}
