import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api';
import { EmiPayment, EmiPaymentRequest } from '../models/emi-payment';

@Injectable({
  providedIn: 'root',
})
export class EmiPaymentService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/loan-accounts`;

  // Full installment schedule (history + pending) for a loan account.
  getSchedule(loanAccountId: number): Observable<EmiPayment[]> {
    return this.http.get<EmiPayment[]>(`${this.baseUrl}/${loanAccountId}/emis`);
  }

  pay(loanAccountId: number, request: EmiPaymentRequest): Observable<EmiPayment> {
    return this.http.post<EmiPayment>(`${this.baseUrl}/${loanAccountId}/emis/pay`, request);
  }
}
