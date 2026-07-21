import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api';
import { LoanAccount } from '../models/loan-account';
import { LoanAccountStatus } from '../models/enums';

@Injectable({
  providedIn: 'root',
})
export class LoanAccountService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${API_BASE_URL}/loan-accounts`;

  // --- Customer ---
  getMyAccounts(): Observable<LoanAccount[]> {
    return this.http.get<LoanAccount[]>(`${this.baseUrl}/my`);
  }

  // --- Manager ---
  getAll(): Observable<LoanAccount[]> {
    return this.http.get<LoanAccount[]>(this.baseUrl);
  }

  getById(id: number): Observable<LoanAccount> {
    return this.http.get<LoanAccount>(`${this.baseUrl}/${id}`);
  }

  getByStatus(status: LoanAccountStatus): Observable<LoanAccount[]> {
    return this.http.get<LoanAccount[]>(`${this.baseUrl}/status/${status}`);
  }

  disburse(id: number): Observable<LoanAccount> {
    return this.http.put<LoanAccount>(`${this.baseUrl}/${id}/disburse`, {});
  }
}
